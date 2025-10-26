package com.algangi.mongle.post.presentation.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminPostUpdateRequest(
    @Size(max = 2000, message = "게시글 내용은 2000자를 초과할 수 없습니다.")
    String content,

    @Size(max = 500, message = "infoText는 500자를 초과할 수 없습니다.")
    String infoText,

    List<String> fileKeyList,

    Boolean isAnonymous
) {
    // fileKeyList는 null일 수 있음 (파일 변경 없을 때)
    // infoText는 null일 수 있음 (infoText 제거 시)
}
