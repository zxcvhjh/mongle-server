package com.algangi.mongle.post.application.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.post.exception.PostErrorCode;
import com.algangi.mongle.post.exception.RateLimitException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostRateLimiter {

    private static final String RATE_LIMIT_KEY_PREFIX = "post-creation:rate-limit:";
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(3);
    private final RedisTemplate<String, String> redisTemplate;

    public void checkRateLimit(String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        if (redisTemplate.hasKey(key)) {
            long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);

            ttlSeconds =
                ttlSeconds < 0 ? BLOCK_DURATION.getSeconds() : ttlSeconds;

            long minutes = ttlSeconds / 60;
            long seconds = ttlSeconds % 60;

            throw new RateLimitException(PostErrorCode.POST_RATE_LIMIT_EXCEEDED, minutes, seconds);
        }
    }

    public void blockUser(String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        redisTemplate.opsForValue().set(key, "blocked", BLOCK_DURATION);
    }
}
