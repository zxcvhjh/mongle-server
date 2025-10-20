package com.algangi.mongle.global.initializer;

import com.algangi.mongle.postViewLog.domain.model.PostViewLog;
import com.algangi.mongle.postViewLog.domain.repository.PostViewLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmer implements ApplicationRunner {

    private final PostViewLogRepository postViewLogRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String VIEWED_POSTS_KEY_PREFIX = "viewed_posts:";
    private static final Duration VIEWED_POSTS_TTL = Duration.ofDays(7);

    @Override
    @Transactional(readOnly = true)
    @SchedulerLock(name = "cacheWarmer", lockAtMostFor = "PT10M")
    public void run(ApplicationArguments args) {
        log.info("[CacheWarmer] 최근 72시간 조회 기록을 Redis에 적재 시작");

        Instant since = Instant.now().minus(72, ChronoUnit.HOURS);

        try (Stream<PostViewLog> recentLogsStream = postViewLogRepository.streamAllByCreatedDateAfterWithJoins(since)) {

            Map<String, Set<String>> viewsByMember = recentLogsStream
                    .filter(log -> log.getMember() != null && log.getPost() != null)
                    .collect(Collectors.groupingBy(
                            log -> log.getMember().getMemberId(),
                            Collectors.mapping(log -> log.getPost().getId(), Collectors.toSet())
                    ));

            if (viewsByMember.isEmpty()) {
                log.info("[CacheWarmer] 적재할 데이터가 없음. 작업 종료");
                return;
            }

            RedisSerializer<String> serializer = redisTemplate.getStringSerializer();
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                viewsByMember.forEach((memberId, postIds) -> {
                    String key = VIEWED_POSTS_KEY_PREFIX + memberId;
                    byte[] serializedKey = serializer.serialize(key);

                    if (serializedKey == null) return;

                    byte[][] serializedPostIds = postIds.stream()
                            .map(serializer::serialize)
                            .toArray(byte[][]::new);

                    connection.setCommands().sAdd(serializedKey, serializedPostIds);
                    connection.keyCommands().expire(serializedKey, VIEWED_POSTS_TTL.toSeconds());
                });
                return null;
            });

            log.info("[CacheWarmer] {}명의 사용자에 대한 조회 기록 적재 완료", viewsByMember.size());

        } catch (Exception e) {
            log.error("[CacheWarmer] 조회 기록 적재 중 오류 발생", e);
        }
    }
}
