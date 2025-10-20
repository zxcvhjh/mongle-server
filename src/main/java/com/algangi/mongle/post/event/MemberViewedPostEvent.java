package com.algangi.mongle.post.event;

public record MemberViewedPostEvent(
        String memberId,
        String postId
) {

}