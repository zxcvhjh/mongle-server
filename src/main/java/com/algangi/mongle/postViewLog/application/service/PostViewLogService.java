package com.algangi.mongle.postViewLog.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class PostViewLogService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> recordViewScript;

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

    private String getKey(String memberId) {
        return VIEWED_POSTS_KEY_PREFIX + memberId;
    }
}