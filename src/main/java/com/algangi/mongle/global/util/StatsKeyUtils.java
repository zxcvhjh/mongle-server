package com.algangi.mongle.global.util;

import com.algangi.mongle.reaction.domain.model.TargetType;
import com.algangi.mongle.stats.domain.StatsKeyPrefix;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StatsKeyUtils {

    private static final String SEPARATOR = "::";

    public static String counterKey(StatsKeyPrefix prefix, TargetType targetType, String id) {
        return String.join(SEPARATOR, prefix.getPrefix(), targetType.getLowerCase(), id);
    }

    public static String trackingKey(StatsKeyPrefix prefix, TargetType type) {
        return String.join(SEPARATOR,
                StatsKeyPrefix.TRACKING.getPrefix(),
                prefix.getPrefix(),
                type.getLowerCase()
        );
    }

    public static String rankingKey(String postId) {
        return String.join(SEPARATOR,
                StatsKeyPrefix.COMMENT_RANKING_BY_LIKES.getPrefix(),
                postId
        );
    }

    public static String extractId(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("잘못된 Redis 키 형식: key가 null이거나 비어있습니다.");
        }

        String[] parts = key.split(SEPARATOR);

        if (parts.length < 2) {
            throw new IllegalArgumentException("ID를 추출할 수 없는 Redis 키 형식입니다: " + key);
        }

        return parts[parts.length - 1];
    }
}