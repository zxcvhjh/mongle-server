package com.algangi.mongle.post.presentation.dto;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
    @NotNull(message = "위도는 필수값입니다.")
    @Min(value = -90, message = "위도는 -90 이상이어야 합니다.")
    @Max(value = 90, message = "위도는 90 이하여야 합니다.")
    Double latitude,
    @NotNull(message = "경도는 필수값입니다.")
    @Min(value = -180, message = "경도는 -180 이상이어야 합니다.")
    @Max(value = 180, message = "경도는 180 이하여야 합니다.")
    Double longitude,
    @NotBlank(message = "게시글 내용은 필수값입니다.")
    @Size(max = 2000)
    String content,
    List<String> fileKeyList,
    boolean isRandomLocationEnabled,
    Boolean isAnonymous
) {

    public PostCreateRequest {
        fileKeyList = Optional.ofNullable(fileKeyList).orElseGet(Collections::emptyList);

    }

}
