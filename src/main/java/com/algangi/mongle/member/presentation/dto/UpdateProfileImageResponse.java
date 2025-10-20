package com.algangi.mongle.member.presentation.dto;

// profileImageUrl은 null일 수 있음 (이미지 삭제 시)
public record UpdateProfileImageResponse(
    String profileImageUrl
) {}