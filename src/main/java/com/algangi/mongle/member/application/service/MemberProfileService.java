package com.algangi.mongle.member.application.service;

import com.algangi.mongle.file.application.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algangi.mongle.file.application.service.FileService;
import com.algangi.mongle.file.application.service.ViewUrlIssueService;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.member.presentation.dto.UpdateProfileImageResponse;
import com.algangi.mongle.member.presentation.dto.UserDetailResponse;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Objects;


@Service
@RequiredArgsConstructor
public class MemberProfileService {

    private final MemberFinder memberFinder;
    private final ViewUrlIssueService viewUrlIssueService;
    private final FileService fileService;

    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetails(String memberId) {
        Member member = memberFinder.getMemberOrThrow(memberId);
        String profileImageKey = member.getProfileImage();
        String profileImageUrl = (profileImageKey != null)
            ? viewUrlIssueService.issueViewUrl(profileImageKey).url()
            : null;

        return UserDetailResponse.of(
            member.getNickname(),
            member.getEmail(),
            profileImageUrl
        );
    }

    /**
     * 사용자의 프로필 이미지를 업데이트하거나 삭제합니다.
     * @param userId 사용자 ID
     * @param newProfileImageKey 새 프로필 이미지 키 (null이나 빈 문자열이면 삭제)
     * @return 업데이트된 프로필 이미지 정보 DTO
     */
    @Transactional
    public UpdateProfileImageResponse updateProfileImage(String userId, String newProfileImageKey) {
        // 1. 사용자 정보 조회 (Pessimistic Lock으로 동시성 문제 방지)
        Member member = memberFinder.getMemberWithLockOrThrow(userId);
        String currentProfileImageKey = member.getProfileImage();

        boolean shouldUpdate = !Objects.equals(currentProfileImageKey, newProfileImageKey);
        String finalProfileImageUrl = null;

        if (shouldUpdate) {
            // 2. 기존 이미지가 있었으면 삭제 처리
            if (StringUtils.hasText(currentProfileImageKey)) {
                // S3 등 저장소에서 실제 파일 삭제 요청
                fileService.deletePermanentFiles(List.of(currentProfileImageKey));
            }

            // 3. 새 이미지 키가 유효하면 (null이나 공백이 아니면)
            if (StringUtils.hasText(newProfileImageKey)) {
                // 새 파일을 영구 파일로 상태 변경 (S3 태그 변경 등)
                fileService.commitFiles(List.of(newProfileImageKey));
                // Member 엔티티에 새 이미지 키 업데이트
                member.updateProfileImage(newProfileImageKey);
                // 응답에 포함될 URL 생성
                finalProfileImageUrl = viewUrlIssueService.issueViewUrl(newProfileImageKey).url();
            } else {
                // 새 이미지 키가 없으면 (삭제 요청) Member 엔티티에서 null로 업데이트
                member.updateProfileImage(null);
                finalProfileImageUrl = null; // 삭제했으므로 URL 없음
            }
            // memberRepository.save(member)는 @Transactional에 의해 자동 처리됨
        } else if (StringUtils.hasText(currentProfileImageKey)) {
            // 변경 사항이 없더라도 현재 이미지 URL을 조회해서 반환
            finalProfileImageUrl = viewUrlIssueService.issueViewUrl(currentProfileImageKey).url();
        }

        // 4. 응답 DTO 생성 및 반환
        return new UpdateProfileImageResponse(finalProfileImageUrl);
    }

}
