package com.algangi.mongle.postViewLog.application.service;

import com.algangi.mongle.postViewLog.domain.repository.PostViewLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
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

    private static final String VIEWED_POSTS_KEY_PREFIX = "viewed_posts:";
    private static final Duration VIEWED_POSTS_TTL = Duration.ofDays(7);

    public void recordView(String memberId, String postId) {
        String key = getKey(memberId);

        redisTemplate.execute(
                recordViewScript,
                Collections.singletonList(key),
                postId,
                String.valueOf(VIEWED_POSTS_TTL.toSeconds())
        );
    }

    public void refreshViewLogTtl(String memberId) {
        String key = getKey(memberId);

        Boolean hasKey = redisTemplate.hasKey(key);
        if (Boolean.TRUE.equals(hasKey)) {
            redisTemplate.expire(key, VIEWED_POSTS_TTL);
        }
    }

    public Set<String> findViewedPostIdsInList(String memberId, List<String> postIds) {
        if (memberId == null || CollectionUtils.isEmpty(postIds)) {
            return Collections.emptySet();
        }

        String key = getKey(memberId);
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
}