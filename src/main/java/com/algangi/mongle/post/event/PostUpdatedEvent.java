package com.algangi.mongle.post.event;

import java.util.List;

public record PostUpdatedEvent(
    String postId,
    List<String> previousFileKeys,
    List<String> finalFileKeys
) {

}