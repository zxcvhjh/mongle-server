package com.algangi.mongle.stats.application.service;

import com.algangi.mongle.global.util.StatsKeyUtils;
import com.algangi.mongle.reaction.domain.model.ReactionType;
import com.algangi.mongle.reaction.domain.model.TargetType;
import com.algangi.mongle.reaction.presentation.dto.ReactionResponse;
import com.algangi.mongle.stats.application.dto.ReactionCleanupDto;
import com.algangi.mongle.stats.domain.StatsKeyPrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"})
public class ContentStatsService {

    private final RedisTemplate<String, String> redisTemplate;

    @Qualifier("reactionScript")
    private final RedisScript<List<Long>> reactionScript;

    @Qualifier("decrAndSaddScript")
    private final RedisScript<Long> decrAndSaddScript;

    @Qualifier("incrAndSaddScript")
    private final RedisScript<Void> incrAndSaddScript;

    @Qualifier("removeReactionAtomicScript")
    private final RedisScript<Long> removeReactionAtomicScript;

    private static final String TRACKING_SET_TTL_SECONDS = "259200";

    public void incrementPostViewCount(String postId) {
        String counterKey = StatsKeyUtils.counterKey(StatsKeyPrefix.VIEWS, TargetType.POST, postId);
        String trackingKey = StatsKeyUtils.trackingKey(StatsKeyPrefix.VIEWS, TargetType.POST);

        redisTemplate.execute(
                incrAndSaddScript,
                List.of(counterKey, trackingKey),
                TRACKING_SET_TTL_SECONDS
        );
    }

    public void incrementPostCommentCount(String postId) {
        String counterKey = StatsKeyUtils.counterKey(StatsKeyPrefix.COMMENTS, TargetType.POST, postId);
        String trackingKey = StatsKeyUtils.trackingKey(StatsKeyPrefix.COMMENTS, TargetType.POST);

        redisTemplate.execute(
                incrAndSaddScript,
                List.of(counterKey, trackingKey),
                TRACKING_SET_TTL_SECONDS
        );
    }

    public void decrementPostCommentCount(String postId) {
        String counterKey = StatsKeyUtils.counterKey(StatsKeyPrefix.COMMENTS, TargetType.POST, postId);
        String trackingKey = StatsKeyUtils.trackingKey(StatsKeyPrefix.COMMENTS, TargetType.POST);

        redisTemplate.execute(
                decrAndSaddScript,
                List.of(counterKey, trackingKey),
                TRACKING_SET_TTL_SECONDS
        );
    }

    public void addCommentToRanking(String postId, String commentId) {
        String key = StatsKeyUtils.rankingKey(postId);
        redisTemplate.opsForZSet().add(key, commentId, 0);
    }

    public ReactionResponse updateReaction(TargetType targetType, String targetId, String memberId, ReactionType reactionType, String postId) {
        // 1. 유효성 검증
        validateReactionInputs(reactionType, targetType);

        // 2. lua 스크립트에서 쓸 키 & 인자 배열 생성
        List<String> keys = buildReactionKeys(targetType, targetId, postId);
        Object[] args = buildReactionArgs(memberId, reactionType, targetType, targetId);

        // 3. lua 스크립트 실행
        Map<String, Long> result = executeReactionScript(keys, args);

        // 4. lua 스크립트 결과 dto 변환
        return parseReactionResult(result);
    }

    public void cleanupStatsForDeletedPost(String postId) {
        if (!StringUtils.hasText(postId)) {
            return;
        }
        List<String> keysToDelete = List.of(
                StatsKeyUtils.counterKey(StatsKeyPrefix.VIEWS, TargetType.POST, postId),
                StatsKeyUtils.counterKey(StatsKeyPrefix.COMMENTS, TargetType.POST, postId),
                StatsKeyUtils.counterKey(StatsKeyPrefix.LIKES, TargetType.POST, postId),
                StatsKeyUtils.counterKey(StatsKeyPrefix.DISLIKES, TargetType.POST, postId),
                StatsKeyUtils.counterKey(StatsKeyPrefix.REACTIONS, TargetType.POST, postId),
                StatsKeyUtils.rankingKey(postId)
        );
        Map<String, String> keysToSrem = Map.of(
                StatsKeyUtils.trackingKey(StatsKeyPrefix.VIEWS, TargetType.POST),
                StatsKeyUtils.counterKey(StatsKeyPrefix.VIEWS, TargetType.POST, postId),
                StatsKeyUtils.trackingKey(StatsKeyPrefix.COMMENTS, TargetType.POST),
                StatsKeyUtils.counterKey(StatsKeyPrefix.COMMENTS, TargetType.POST, postId),
                StatsKeyUtils.trackingKey(StatsKeyPrefix.LIKES, TargetType.POST),
                StatsKeyUtils.counterKey(StatsKeyPrefix.LIKES, TargetType.POST, postId),
                StatsKeyUtils.trackingKey(StatsKeyPrefix.DISLIKES, TargetType.POST),
                StatsKeyUtils.counterKey(StatsKeyPrefix.DISLIKES, TargetType.POST, postId)
        );
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                var serializer = redisTemplate.getStringSerializer();
                for (String key : keysToDelete) {
                    connection.keyCommands().del(serializer.serialize(key));
                }
                for (var entry : keysToSrem.entrySet()) {
                    connection.setCommands().sRem(serializer.serialize(entry.getKey()), serializer.serialize(entry.getValue()));
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to cleanup stats for deleted post: {}", postId, e);
        }
    }

    public void cleanupStatsForDeletedComment(String commentId, String postId) {
        if (!StringUtils.hasText(commentId)) {
            return;
        }
        List<String> keysToDelete = List.of(
                StatsKeyUtils.counterKey(StatsKeyPrefix.LIKES, TargetType.COMMENT, commentId),
                StatsKeyUtils.counterKey(StatsKeyPrefix.DISLIKES, TargetType.COMMENT, commentId),
                StatsKeyUtils.counterKey(StatsKeyPrefix.REACTIONS, TargetType.COMMENT, commentId)
        );
        Map<String, String> keysToSrem = Map.of(
                StatsKeyUtils.trackingKey(StatsKeyPrefix.LIKES, TargetType.COMMENT),
                StatsKeyUtils.counterKey(StatsKeyPrefix.LIKES, TargetType.COMMENT, commentId),
                StatsKeyUtils.trackingKey(StatsKeyPrefix.DISLIKES, TargetType.COMMENT),
                StatsKeyUtils.counterKey(StatsKeyPrefix.DISLIKES, TargetType.COMMENT, commentId)
        );
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                var serializer = redisTemplate.getStringSerializer();
                for (String key : keysToDelete) {
                    connection.keyCommands().del(serializer.serialize(key));
                }
                for (var entry : keysToSrem.entrySet()) {
                    connection.setCommands().sRem(serializer.serialize(entry.getKey()), serializer.serialize(entry.getValue()));
                }
                if (StringUtils.hasText(postId)) {
                    connection.zSetCommands().zRem(
                            serializer.serialize(StatsKeyUtils.rankingKey(postId)),
                            serializer.serialize(commentId)
                    );
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to cleanup stats for deleted comment: {}", commentId, e);
        }
    }

    public void removeReactionsFromRedis(String memberId, List<ReactionCleanupDto> reactions) {
        if (isInvalidReactionCleanupArgs(memberId, reactions)) {
            return;
        }

        try {
            for (ReactionCleanupDto reaction : reactions) {
                List<String> keys = buildRemoveReactionKeys(reaction);
                Object[] args = buildRemoveReactionArgs(memberId, reaction);
                redisTemplate.execute(removeReactionAtomicScript, keys, args);
            }
        } catch (Exception e) {
            log.error("Failed to execute removeReactionsFromRedis for memberId: {}", memberId, e);
            throw new RuntimeException("Redis reaction cleanup failed", e);
        }
    }

    private boolean isInvalidReactionCleanupArgs(String memberId, List<ReactionCleanupDto> reactions) {
        if (!StringUtils.hasText(memberId)) {
            log.warn("Cannot remove reactions: memberId is blank");
            return true;
        }

        if (reactions == null || reactions.isEmpty()) {
            log.warn("Cannot remove reactions: reaction list is empty for memberId {}", memberId);
            return true;
        }

        return false;
    }

    private List<String> buildRemoveReactionKeys(ReactionCleanupDto reaction) {
        TargetType targetType = reaction.targetType();
        String targetId = reaction.targetId();
        StatsKeyPrefix prefix = (reaction.reactionType() == ReactionType.LIKE) ? StatsKeyPrefix.LIKES : StatsKeyPrefix.DISLIKES;

        List<String> keys = new ArrayList<>();
        keys.add(StatsKeyUtils.counterKey(StatsKeyPrefix.REACTIONS, targetType, targetId));
        keys.add(StatsKeyUtils.counterKey(prefix, targetType, targetId));
        keys.add(StatsKeyUtils.trackingKey(prefix, targetType));

        if (targetType == TargetType.COMMENT && reaction.reactionType() == ReactionType.LIKE && StringUtils.hasText(reaction.postId())) {
            keys.add(StatsKeyUtils.rankingKey(reaction.postId()));
        } else {
            keys.add("");
        }
        return keys;
    }

    private Object[] buildRemoveReactionArgs(String memberId, ReactionCleanupDto reaction) {
        return new Object[] {
                memberId,
                reaction.reactionType().name(),
                reaction.targetType().name(),
                reaction.targetId()
        };
    }

    private void validateReactionInputs(ReactionType reactionType, TargetType targetType) {
        if (reactionType == null) {
            throw new IllegalArgumentException("reactionType must not be null");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("targetType must not be null");
        }
    }

    private List<String> buildReactionKeys(TargetType targetType, String targetId, String postId) {
        List<String> keys = new ArrayList<>();

        keys.add(StatsKeyUtils.counterKey(StatsKeyPrefix.REACTIONS, targetType, targetId));
        keys.add(StatsKeyUtils.counterKey(StatsKeyPrefix.LIKES, targetType, targetId));
        keys.add(StatsKeyUtils.counterKey(StatsKeyPrefix.DISLIKES, targetType, targetId));

        if (targetType == TargetType.COMMENT) {
            if (!StringUtils.hasText(postId)) {
                log.warn("COMMENT 리액션이지만 postId가 없습니다. targetId: {}", targetId);
                keys.add("");
            } else {
                keys.add(StatsKeyUtils.rankingKey(postId));
            }
        } else {
            keys.add("");
        }

        keys.add(StatsKeyUtils.trackingKey(StatsKeyPrefix.LIKES, targetType));
        keys.add(StatsKeyUtils.trackingKey(StatsKeyPrefix.DISLIKES, targetType));

        return keys;
    }

    private Object[] buildReactionArgs(String memberId, ReactionType reactionType, TargetType targetType, String targetId) {
        return new Object[]{
                memberId,
                reactionType.name(),
                targetType.name(),
                targetId,
                TRACKING_SET_TTL_SECONDS
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> executeReactionScript(List<String> keys, Object[] args) {
        Object rawResult = redisTemplate.execute(reactionScript, keys, args);

        if (!(rawResult instanceof List)) {
            log.error("Lua 스크립트 결과 타입 오류. 기대값: List, 실제값: {}, keys: {}, args: {}",
                    rawResult != null ? rawResult.getClass().getName() : "null",
                    keys, args);

            throw new IllegalStateException("Redis Lua 스크립트 실행 결과가 List 타입이 아닙니다. (실제 결과: " + rawResult + ")");
        }

        List<?> list = (List<?>) rawResult;

        Long likes = extractLong(list.get(0));
        Long dislikes = extractLong(list.get(1));

        return Map.of("likes", likes, "dislikes", dislikes);
    }

    private Long extractLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private ReactionResponse parseReactionResult(Map<String, Long> result) {
        return new ReactionResponse(
                result.get("likes"),
                result.get("dislikes")
        );
    }
}
