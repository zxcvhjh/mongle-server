package com.algangi.mongle.comment.domain.model;

import com.algangi.mongle.global.constants.ReportConstants;
import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.post.domain.model.Location;
import com.algangi.mongle.post.domain.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comment 엔티티의 신고 기능 테스트
 */
class CommentReportTest {

    private Post mockPost;

    @BeforeEach
    void setUp() {
        mockPost = Post.createStandalone(
            Location.create(37.5, 127.0),
            "test-s2-token",
            "test post content",
            "post-author-id",
            false
        );
    }

    @Test
    @DisplayName("ACTIVE 상태의 댓글 신고 시 reportCount가 증가한다")
    void incrementReportCount_WhenActive() {
        // given
        Comment comment = createActiveComment();

        // when
        comment.incrementReportCountAndBlockIfNeeded();

        // then
        assertThat(comment.getReportCount()).isEqualTo(1);
        assertThat(comment.getStatus()).isEqualTo(CommentStatus.ACTIVE);
    }

    @Test
    @DisplayName("신고 횟수가 REPORT_BLOCK_THRESHOLD에 도달하면 BLOCKED_BY_REPORTS 상태로 변경된다")
    void incrementReportCount_BlocksWhenThresholdReached() {
        // given
        Comment comment = createActiveComment();

        // when - REPORT_BLOCK_THRESHOLD(5)번 신고
        for (int i = 0; i < ReportConstants.REPORT_BLOCK_THRESHOLD; i++) {
            comment.incrementReportCountAndBlockIfNeeded();
        }

        // then
        assertThat(comment.getReportCount()).isEqualTo(ReportConstants.REPORT_BLOCK_THRESHOLD);
        assertThat(comment.getStatus()).isEqualTo(CommentStatus.BLOCKED_BY_REPORTS);
    }

    @Test
    @DisplayName("신고 횟수가 threshold - 1일 때는 ACTIVE 상태를 유지한다")
    void incrementReportCount_StaysActiveBeforeThreshold() {
        // given
        Comment comment = createActiveComment();

        // when - threshold - 1번 신고
        for (int i = 0; i < ReportConstants.REPORT_BLOCK_THRESHOLD - 1; i++) {
            comment.incrementReportCountAndBlockIfNeeded();
        }

        // then
        assertThat(comment.getReportCount()).isEqualTo(ReportConstants.REPORT_BLOCK_THRESHOLD - 1);
        assertThat(comment.getStatus()).isEqualTo(CommentStatus.ACTIVE);
    }

    @Test
    @DisplayName("ACTIVE가 아닌 상태의 댓글을 신고하면 예외가 발생한다")
    void incrementReportCount_ThrowsExceptionWhenNotActive() {
        // given
        Comment comment = createCommentWithStatus(CommentStatus.DELETED_BY_USER);

        // when & then
        assertThatThrownBy(() -> comment.incrementReportCountAndBlockIfNeeded())
            .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("이미 차단된 댓글을 신고하면 예외가 발생한다")
    void incrementReportCount_ThrowsExceptionWhenAlreadyBlocked() {
        // given
        Comment comment = createCommentWithStatus(CommentStatus.BLOCKED_BY_REPORTS);

        // when & then
        assertThatThrownBy(() -> comment.incrementReportCountAndBlockIfNeeded())
            .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("ReportConstants의 REPORT_BLOCK_THRESHOLD와 실제 차단 로직이 일치한다")
    void reportThreshold_ConsistencyCheck() {
        // given
        Comment comment = createActiveComment();

        // when - threshold 직전까지 신고
        for (int i = 0; i < ReportConstants.REPORT_BLOCK_THRESHOLD - 1; i++) {
            comment.incrementReportCountAndBlockIfNeeded();
        }
        assertThat(comment.getStatus()).isEqualTo(CommentStatus.ACTIVE);

        // when - threshold 도달
        comment.incrementReportCountAndBlockIfNeeded();

        // then
        assertThat(comment.getStatus()).isEqualTo(CommentStatus.BLOCKED_BY_REPORTS);
    }

    private Comment createActiveComment() {
        Comment comment = Comment.createParentComment(
            "test comment content",
            mockPost,
            null,  // member (익명 댓글)
            false  // isAnonymous
        );

        // Comment 생성 시 기본 상태가 ACTIVE
        return comment;
    }

    private Comment createCommentWithStatus(CommentStatus status) {
        Comment comment = createActiveComment();
        ReflectionTestUtils.setField(comment, "status", status);
        return comment;
    }
}
