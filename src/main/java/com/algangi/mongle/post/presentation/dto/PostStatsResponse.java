package com.algangi.mongle.post.presentation.dto;

import com.algangi.mongle.stats.application.dto.PostStats;

/**
 * 게시글 통계 조회 응답
 * 조회수를 증가시키지 않고 통계 정보만 반환
 */
public record PostStatsResponse(
        long viewCount,
        long commentCount,
        long likeCount,
        long dislikeCount
) {

    public static PostStatsResponse from(PostStats stats) {
        return new PostStatsResponse(
                stats.viewCount(),
                stats.commentCount(),
                stats.likeCount(),
                stats.dislikeCount()
        );
    }
}
