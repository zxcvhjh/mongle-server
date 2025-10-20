package com.algangi.mongle.member.presentation.dto;

// fileKey는 null일 수 있음 (이미지 삭제 요청)
public record UpdateProfileImageRequest(
    String fileKey
) {}