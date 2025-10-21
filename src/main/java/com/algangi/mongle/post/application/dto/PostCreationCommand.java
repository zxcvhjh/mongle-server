package com.algangi.mongle.post.application.dto;

import com.algangi.mongle.post.domain.model.Location;

public record PostCreationCommand(
    Location location,
    String s2TokenId,
    String content,
    String authorId,
    boolean isAnonymous
) {

    public static PostCreationCommand of(Location location, String s2TokenId,
        String content, String authorId, boolean isAnonymous){
        return new PostCreationCommand(location, s2TokenId, content, authorId, isAnonymous);
    }

}
