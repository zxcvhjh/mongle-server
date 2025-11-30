package com.algangi.mongle.global.config;

import java.util.List;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisProperties redisProperties;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisProperties.getHost());
        config.setPort(redisProperties.getPort());
        config.setPassword(redisProperties.getPassword());
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        return template;
    }

    @Bean
    @SuppressWarnings({"rawtypes", "unchecked"})
    public RedisScript<List<Long>> reactionScript() {
        DefaultRedisScript<List> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("redis/reaction.lua"))
        );
        redisScript.setResultType(List.class);
        return (RedisScript<List<Long>>) (RedisScript) redisScript;
    }

    @Bean
    public RedisScript<Long> removeReactionAtomicScript() {
        ClassPathResource scriptResource = new ClassPathResource("redis/remove_reaction_atomic.lua");
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(scriptResource));
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    @Bean
    public RedisScript<Long> decrAndSaddScript() {
        ClassPathResource scriptResource = new ClassPathResource("redis/decr_sadd_expire_nx.lua");
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(scriptResource));
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    @Bean
    public RedisScript<Void> incrAndSaddScript() {
        ClassPathResource scriptResource = new ClassPathResource("redis/incr_sadd_expire_nx.lua");
        DefaultRedisScript<Void> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(new ResourceScriptSource(scriptResource));
        redisScript.setResultType(Void.class);
        return redisScript;
    }

    @Bean
    public RedisScript<List> getReactionsScript() {
        Resource scriptSource = new ClassPathResource("redis/get_reactions.lua");
        return RedisScript.of(scriptSource, List.class);
    }

    @Bean
    public RedisScript<Long> recordViewScript() {
        Resource scriptSource = new ClassPathResource("redis/record_view.lua");
        return RedisScript.of(scriptSource, Long.class);
    }

    @Bean
    public RedisScript<Long> unlockScript() {
        Resource scriptSource = new ClassPathResource("redis/unlock_if_value_matches.lua");
        return RedisScript.of(scriptSource, Long.class);
    }
}