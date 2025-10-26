package com.algangi.mongle.post.presentation.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminPostUpdateRequest(
    @Size(max = 2000, message = "게시글 내용은 2000자를 초과할 수 없습니다.")
    String content,

    @Size(max = 2000, message = "infoText는 2000자를 초과할 수 없습니다.")
    String infoText,

    List<String> fileKeyList,

    Boolean isAnonymous,

    @Size(max = 255, message = "커스텀 닉네임은 255자를 초과할 수 없습니다.")
    String customNickname
) {

}

