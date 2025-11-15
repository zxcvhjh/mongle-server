package com.algangi.mongle.post.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record AdminPostCreateRequest(
    @NotNull(message = "위도는 필수값입니다.")
    Double latitude,

    @NotNull(message = "경도는 필수값입니다.")
    Double longitude,

    @NotBlank(message = "게시글 내용은 필수값입니다.")
    @Size(max = 2000, message = "게시글 내용은 2000자를 초과할 수 없습니다.")
    String content,

    @Size(max = 2000, message = "infoText는 2000자를 초과할 수 없습니다.")
    String infoText,

    List<String> fileKeyList,

    boolean isRandomLocationEnabled,

    Boolean isAnonymous,

    @Size(max = 255, message = "커스텀 닉네임은 255자를 초과할 수 없습니다.")
    String customNickname
) {

    public AdminPostCreateRequest {
        fileKeyList = Optional.ofNullable(fileKeyList).orElseGet(Collections::emptyList);
    }
}

