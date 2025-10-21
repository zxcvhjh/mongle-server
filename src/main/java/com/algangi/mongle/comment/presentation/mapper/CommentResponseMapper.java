package com.algangi.mongle.comment.presentation.mapper;
import lombok.RequiredArgsConstructor;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.algangi.mongle.comment.domain.model.Comment;
import com.algangi.mongle.comment.presentation.dto.AuthorInfoResponse;
import com.algangi.mongle.comment.presentation.dto.CommentInfoResponse;
import com.algangi.mongle.file.application.service.ViewUrlIssueService;
import com.algangi.mongle.member.domain.model.Member;

@Component
@RequiredArgsConstructor
public final class CommentResponseMapper {

    private final ViewUrlIssueService viewUrlIssueService;

    private static final String MASKED_CONTENT = "삭제된 댓글입니다.";
    private static final String MASKED_NICKNAME = "(알 수 없음)";
    private static final String ANONYMOUS_NICKNAME = "익명의 몽글러";

    public CommentInfoResponse toCommentInfoResponse(
        Comment comment,
        String currentMemberId,
        boolean hasReplies,
        long likeCount,
        long dislikeCount,
        String myReaction) {

        boolean isAnonymous = comment.isAnonymous();
        boolean isDeleted = comment.isDeleted();
        Member author = comment.getMember();

        AuthorInfoResponse authorInfo = mapAuthor(author, isDeleted, isAnonymous);
        boolean isAuthor = mapIsAuthor(author, currentMemberId, isDeleted);
        String content = mapContent(comment, isDeleted);
        long finalLikeCount = mapCount(likeCount, isDeleted);
        long finalDislikeCount = mapCount(dislikeCount, isDeleted);
        String finalMyReaction = mapMyReaction(myReaction, isDeleted);


        return CommentInfoResponse.builder()
            .commentId(comment.getId())
            .content(content)
            .author(authorInfo)
            .likeCount(finalLikeCount)
            .dislikeCount(finalDislikeCount)
            .myReaction(finalMyReaction)
            .createdAt(comment.getCreatedDate())
            .isAuthor(isAuthor)
            .isDeleted(isDeleted)
            .hasReplies(hasReplies)
            .build();
    }

    private String mapContent(Comment comment, boolean deleted) {
        return deleted ? MASKED_CONTENT : comment.getContent();
    }

    private AuthorInfoResponse mapAuthor(Member author, boolean deleted, boolean isAnonymous) {
        if (deleted || author == null) {
            return new AuthorInfoResponse(null, MASKED_NICKNAME, null);
        }
        if (isAnonymous) {
            return new AuthorInfoResponse(null, ANONYMOUS_NICKNAME, null);
        }

        String profileImageUrl = null;
        String profileImageKey = author.getProfileImage();

        if (profileImageKey != null) {
            try {
                profileImageUrl = viewUrlIssueService.issueViewUrl(profileImageKey).url();
            } catch (Exception e) {
            }
        }

        return new AuthorInfoResponse(
            author.getMemberId(),
            author.getNickname(),
            profileImageUrl
        );
    }

    private boolean mapIsAuthor(Member author, String currentMemberId, boolean deleted) {
        return !deleted
            && author != null
            && Objects.equals(author.getMemberId(), currentMemberId);
    }

    private long mapCount(long count, boolean deleted) {
        return deleted ? 0 : count;
    }

    private String mapMyReaction(String myReaction, boolean isDeleted) {
        return isDeleted ? null : myReaction;
    }

}