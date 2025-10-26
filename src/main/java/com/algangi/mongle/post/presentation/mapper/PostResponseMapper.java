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
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostResponseMapper {

    private final ViewUrlIssueService viewUrlIssueService;

    public PostListResponse.PostSummary toPostSummary(
        Post post,
        Member author,
        List<String> photoUrls,
        PostStats stats,
        String myReaction
    ) {
        String authorId = post.getAuthorId();
        String customNickname = post.getCustomNickname();
        boolean isAnonymous = post.isAnonymous();
        String profileImageUrl = null;
        String finalNickname;

        if (author != null && post.getStatus() == PostStatus.ACTIVE
            && author.getProfileImage() != null) {
            try {
                profileImageUrl = viewUrlIssueService.issueViewUrl(author.getProfileImage()).url();
            } catch (Exception e) {
                log.warn("Failed to issue view URL for profile image key in PostResponseMapper: {}",
                    author.getProfileImage(), e);
            }
        }

        if (author == null) {
            finalNickname = "익명의 몽글러";
            profileImageUrl = null;
        } else if (StringUtils.hasText(customNickname)) {
            finalNickname = customNickname;
        } else if (isAnonymous) {
            finalNickname = "익명의 몽글러";
            profileImageUrl = null;
        } else {
            finalNickname = author.getNickname();
        }

        PostListResponse.PostSummary.Author authorDto = new PostListResponse.PostSummary.Author(
            authorId,
            finalNickname,
            profileImageUrl
        );

        return new PostListResponse.PostSummary(
            post.getId(),
            authorDto,
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

