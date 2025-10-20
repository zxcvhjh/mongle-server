package com.algangi.mongle.postViewLog.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PostViewLogService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<Long> recordViewScript;

    private static final String VIEWED_POSTS_KEY_PREFIX = "viewed_posts:";
    private static final long VIEWED_POSTS_TTL_SECONDS = TimeUnit.DAYS.toSeconds(7);

    public void recordView(String memberId, String postId) {
        String key = VIEWED_POSTS_KEY_PREFIX + memberId;

        redisTemplate.execute(
                recordViewScript,
                Collections.singletonList(key),
                postId,
                String.valueOf(VIEWED_POSTS_TTL_SECONDS)
        );
    }
}