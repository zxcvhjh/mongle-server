package com.algangi.mongle.postViewLog.application.service;

import com.algangi.mongle.postViewLog.domain.model.PostViewLog;
import com.algangi.mongle.postViewLog.domain.repository.PostViewLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostViewLogService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> recordViewScript;
    private final PostViewLogRepository postViewLogRepository;
    private final RedisScript<Long> unlockScript;

    private static final String VIEWED_POSTS_KEY_PREFIX = "viewed_posts:";
    private static final String LOCK_KEY_PREFIX = "lock:view_log:";
    private static final Duration VIEWED_POSTS_TTL = Duration.ofDays(7);
    private static final Duration LAZY_LOAD_LOCK_TTL = Duration.ofSeconds(10);
    private static final Duration EMPTY_CACHE_TTL = Duration.ofMinutes(10);

    public void recordView(String memberId, String postId) {
        if (memberId == null || memberId.isBlank() || postId == null || postId.isBlank()) {
            log.warn("recordView called with invalid args. memberId='{}', postId='{}'", memberId, postId);
            return;
        }

        String key = getKey(memberId);

        redisTemplate.execute(
                recordViewScript,
                Collections.singletonList(key),
                postId,
                String.valueOf(VIEWED_POSTS_TTL.toSeconds())
        );
    }

    public void refreshViewLogTtl(String memberId) {
        if (StringUtils.hasText(memberId)) {
            String key = getKey(memberId);
            redisTemplate.expire(key, VIEWED_POSTS_TTL);
        }
    }

    public Set<String> findViewedPostIdsInList(String memberId, List<String> postIds) {
        if (!StringUtils.hasText(memberId) || CollectionUtils.isEmpty(postIds)) {
            return Collections.emptySet();
        }

        String key = getKey(memberId);

        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            lazyLoadViewLogsIntoCache(memberId);
        }

        var serializer = redisTemplate.getStringSerializer();
        List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (String postId : postIds) {
                connection.setCommands().sIsMember(serializer.serialize(key), serializer.serialize(postId));
            }
            return null;
        });

        Set<String> viewedPostIds = new HashSet<>();
        for (int i = 0; i < results.size(); i++) {
            if (Boolean.TRUE.equals(results.get(i))) {
                viewedPostIds.add(postIds.get(i));
            }
        }
        return viewedPostIds;
    }

    public void cleanupViewLogs(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            log.warn("cleanupViewLogs called with null or blank memberId");
            return;
        }

        redisTemplate.delete(getKey(memberId));
        postViewLogRepository.deleteAllByMemberId(memberId);
    }

    private String getKey(String memberId) {
        return VIEWED_POSTS_KEY_PREFIX + memberId;
    }

    private String getLockKey(String memberId) {
        return LOCK_KEY_PREFIX + memberId;
    }

    private void lazyLoadViewLogsIntoCache(String memberId) {
        String lockKey = getLockKey(memberId);
        String lockToken = java.util.UUID.randomUUID().toString();
        Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockToken, LAZY_LOAD_LOCK_TTL);

        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                String cacheKey = getKey(memberId);
                if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
                    return;
                }

                log.debug("[Lazy Loading] Cache miss. DB에서 조회 기록을 가져옵니다. memberId={}", memberId);
                Instant since = Instant.now().minus(14, ChronoUnit.DAYS);
                List<PostViewLog> recentLogs = postViewLogRepository.findByMember_MemberIdAndCreatedDateAfter(memberId, since);

                if (!recentLogs.isEmpty()) {
                    String[] viewedIds = recentLogs.stream()
                            .map(log -> log.getPost().getId())
                            .toArray(String[]::new);

                    redisTemplate.opsForSet().add(cacheKey, viewedIds);
                    redisTemplate.expire(cacheKey, VIEWED_POSTS_TTL);
                } else {
                    final String EMPTY_SENTINEL = "__empty__";
                    redisTemplate.opsForSet().add(cacheKey, EMPTY_SENTINEL);
                    redisTemplate.expire(cacheKey, EMPTY_CACHE_TTL);
                }
            } finally {
                redisTemplate.execute(unlockScript, Collections.singletonList(lockKey), lockToken);
            }
        }
    }
}