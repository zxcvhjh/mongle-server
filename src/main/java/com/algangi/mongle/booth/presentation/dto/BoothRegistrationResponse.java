package com.algangi.mongle.booth.presentation.dto;

import com.algangi.mongle.member.domain.model.Member;

public record BoothRegistrationResponse(
    String email,
    String id,
    String nickname,
    String profileImage
) {

    public static BoothRegistrationResponse of(Member boothAccount) {
        return new BoothRegistrationResponse(boothAccount.getEmail(), boothAccount.getMemberId(),
            boothAccount.getNickname(),
            boothAccount.getProfileImage());
    }

}
