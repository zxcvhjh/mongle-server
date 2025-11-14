package com.algangi.mongle.auth.presentation.dto;

import com.algangi.mongle.global.annotation.ValidEmail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @ValidEmail
    String email,
    @NotBlank(message = "비밀번호는 필수값입니다.")
    @Size(max = 72, message = "비밀번호는 72자 이하여야 합니다.")
    String password
) {

}
