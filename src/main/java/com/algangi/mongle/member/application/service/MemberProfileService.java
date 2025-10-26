package com.algangi.mongle.member.application.service;

import com.algangi.mongle.file.application.service.FileService;
import com.algangi.mongle.member.presentation.dto.UpdateNicknameRequest;
import com.algangi.mongle.member.presentation.dto.UpdateNicknameResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.algangi.mongle.file.application.service.ViewUrlIssueService;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.member.presentation.dto.UpdateProfileImageResponse;
import com.algangi.mongle.member.presentation.dto.UserDetailResponse;
import lombok.RequiredArgsConstructor;

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

    @Transactional
    public UpdateProfileImageResponse updateProfileImage(String userId, String newProfileImageKey) {
        Member member = memberFinder.getMemberWithLockOrThrow(userId);
        String currentProfileImageKey = member.getProfileImage();

        boolean shouldUpdate = !Objects.equals(currentProfileImageKey, newProfileImageKey);
        String finalProfileImageUrl = null;

        if (shouldUpdate) {
            if (StringUtils.hasText(currentProfileImageKey)) {
                fileService.deletePermanentFiles(List.of(currentProfileImageKey));
            }

            if (StringUtils.hasText(newProfileImageKey)) {
                fileService.commitFiles(List.of(newProfileImageKey));
                member.updateProfileImage(newProfileImageKey);
                finalProfileImageUrl = viewUrlIssueService.issueViewUrl(newProfileImageKey).url();
            } else {
                member.updateProfileImage(null);
                finalProfileImageUrl = null;
            }
        } else if (StringUtils.hasText(currentProfileImageKey)) {
            finalProfileImageUrl = viewUrlIssueService.issueViewUrl(currentProfileImageKey).url();
        }

        return new UpdateProfileImageResponse(finalProfileImageUrl);
    }

    @Transactional
    public UpdateNicknameResponse updateNickname(String userId, String newNickname) {
        memberFinder.validateDuplicateNickName(newNickname);

        Member member = memberFinder.getMemberWithLockOrThrow(userId);
        
        member.updateNickname(newNickname);

        return new UpdateNicknameResponse(member.getNickname());
    }
}
