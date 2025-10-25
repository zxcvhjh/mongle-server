package com.algangi.mongle.booth.presentation.dto;

import com.algangi.mongle.member.domain.model.Member;

public record BoothRegistrationResponse(
    String email,
    String id,
    String password,
    String nickname,
    String profileImage
) {

    public static BoothRegistrationResponse of(Member boothAccount, String password) {
        return new BoothRegistrationResponse(boothAccount.getEmail(), boothAccount.getMemberId(),
            password, boothAccount.getNickname(),
            boothAccount.getProfileImage());
    }

}
