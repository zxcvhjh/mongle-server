package com.algangi.mongle.post.event;

import java.util.List;

public record PostCreatedEvent(
    String postId,
    List<String> postFileKeys
) {

}
