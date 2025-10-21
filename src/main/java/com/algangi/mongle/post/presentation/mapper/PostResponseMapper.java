package com.algangi.mongle.post.presentation.mapper;

import com.algangi.mongle.file.application.service.ViewUrlIssueService;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.post.domain.model.PostStatus;
import com.algangi.mongle.post.presentation.dto.PostListResponse;
import com.algangi.mongle.stats.application.dto.PostStats;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostResponseMapper {

    private final ViewUrlIssueService viewUrlIssueService;

    // PostListResponse.PostSummary DTO로 변환하는 메서드
    public PostListResponse.PostSummary toPostSummary(
        Post post,
        Member author, // Nullable
        List<String> photoUrls,
        PostStats stats,
        String myReaction // Nullable
    ) {
        boolean isAnonymous = post.getIsAnonymous();
        PostListResponse.PostSummary.Author authorDto;

        if (author == null) {
            authorDto = new PostListResponse.PostSummary.Author(null, "익명의 몽글러", null);
        } else if (isAnonymous) {
            authorDto = new PostListResponse.PostSummary.Author(author.getMemberId(), "익명의 몽글러", null);
        } else {
            // 프로필 이미지 URL 생성 로직
            String profileImageUrl = null;
            if (post.getStatus() == PostStatus.ACTIVE && author.getProfileImage() != null) {
                try {
                    profileImageUrl = viewUrlIssueService.issueViewUrl(author.getProfileImage()).url();
                } catch (Exception e) {
                    log.warn("Failed to issue view URL for profile image key in PostResponseMapper: {}", author.getProfileImage(), e);
                    // 실패 시 null 유지
                }
            }
            authorDto = new PostListResponse.PostSummary.Author(author.getMemberId(), author.getNickname(), profileImageUrl);
        }

        // 최종 PostSummary DTO 생성
        return new PostListResponse.PostSummary(
            post.getId(),
            authorDto, // 생성된 authorDto 사용
            post.getContent(),
            photoUrls,
            stats.likeCount(),
            stats.dislikeCount(),
            myReaction,
            stats.commentCount(),
            stats.viewCount(),
            post.getCreatedDate(),
            post.getUpdatedDate()
        );
    }
}