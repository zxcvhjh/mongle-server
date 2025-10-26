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
    @Size(max = 2000)
    String content,

    @Size(max = 500, message = "정보 텍스트는 500자를 초과할 수 없습니다.")
    String infoText,

    List<String> fileKeyList,

    Boolean isRandomLocationEnabled,

    Boolean isAnonymous
) {

    public AdminPostCreateRequest {
        fileKeyList = Optional.ofNullable(fileKeyList).orElse(Collections.emptyList());
        isRandomLocationEnabled = Optional.ofNullable(isRandomLocationEnabled).orElse(false);
        isAnonymous = Optional.ofNullable(isAnonymous).orElse(false);
    }
}

