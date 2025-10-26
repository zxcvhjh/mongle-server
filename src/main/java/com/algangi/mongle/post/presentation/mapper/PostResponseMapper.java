package com.algangi.mongle.post.presentation.mapper;

import com.algangi.mongle.file.application.service.ViewUrlIssueService;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.post.domain.model.PostStatus;
import com.algangi.mongle.post.presentation.dto.PostListResponse;
import com.algangi.mongle.stats.application.dto.PostStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

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
        boolean isAnonymous = post.isAnonymous();
        String customNickname = post.getCustomNickname();
        PostListResponse.PostSummary.Author authorDto;
        String authorId = null;
        String nickname = "익명의 몽글러";
        String profileImageUrl = null;

        if (author != null) {
            authorId = author.getMemberId();
            if (StringUtils.hasText(customNickname)) {
                nickname = customNickname;
            } else if (isAnonymous) {
                nickname = "익명의 몽글러";
            } else {
                nickname = author.getNickname();
                if (post.getStatus() == PostStatus.ACTIVE && author.getProfileImage() != null) {
                    try {
                        profileImageUrl = viewUrlIssueService.issueViewUrl(author.getProfileImage())
                            .url();
                    } catch (Exception e) {
                        log.warn(
                            "Failed to issue view URL for profile image key in PostResponseMapper: {}",
                            author.getProfileImage(), e);
                    }
                }
            }
        } else if (StringUtils.hasText(customNickname)) {
            nickname = customNickname;
        }

        authorDto = new PostListResponse.PostSummary.Author(authorId, nickname, profileImageUrl);

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

