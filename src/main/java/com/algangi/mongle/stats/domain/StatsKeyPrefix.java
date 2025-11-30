package com.algangi.mongle.stats.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatsKeyPrefix {
    VIEWS("views"),
    COMMENTS("comments"),
    LIKES("likes"),
    DISLIKES("dislikes"),
    REACTIONS("reactions"),
    COMMENT_RANKING_BY_LIKES("ranking::comments_by_likes::post"),
    TRACKING("tracking");

    private final String prefix;
}
