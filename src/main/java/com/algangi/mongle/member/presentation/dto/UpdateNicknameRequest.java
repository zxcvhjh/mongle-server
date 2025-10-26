package com.algangi.mongle.member.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
    @NotBlank(message = "새 닉네임은 필수값입니다.")
    @Size(min = 1, max = 20, message = "닉네임은 1자 이상 20자 이하로 입력해주세요.")
    String nickname
) {

}
