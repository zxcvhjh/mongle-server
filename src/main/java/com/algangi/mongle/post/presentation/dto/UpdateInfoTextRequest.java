package com.algangi.mongle.post.presentation.dto;

import jakarta.validation.constraints.Size;

public record UpdateInfoTextRequest(
    @Size(max = 2000, message = "infoText는 2000자를 초과할 수 없습니다.")
    String infoText
) {

}