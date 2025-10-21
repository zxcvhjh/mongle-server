package com.algangi.mongle.post.presentation.dto;

public record PostUpdateResponse(
    String id,
    String content,
    boolean isAnonymous
) {

    public static PostUpdateResponse of(String id, String content, boolean isAnonymous) {
        return new PostUpdateResponse(id, content, isAnonymous);
    }
}
