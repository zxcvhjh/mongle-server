package com.algangi.mongle.post.domain.model;

import com.algangi.mongle.global.constants.ReportConstants;
import com.algangi.mongle.global.exception.ApplicationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Post 엔티티의 신고 기능 테스트
 */
class PostReportTest {

    @Test
    @DisplayName("ACTIVE 상태의 게시글 신고 시 reportCount가 증가한다")
    void incrementReportCount_WhenActive() {
        // given
        Post post = createActivePost();

        // when
        post.incrementReportCountAndBlockIfNeeded();

        // then
        assertThat(post.getReportCount()).isEqualTo(1);
        assertThat(post.getStatus()).isEqualTo(PostStatus.ACTIVE);
    }

    @Test
    @DisplayName("신고 횟수가 REPORT_BLOCK_THRESHOLD에 도달하면 BLOCKED_BY_REPORTS 상태로 변경된다")
    void incrementReportCount_BlocksWhenThresholdReached() {
        // given
        Post post = createActivePost();

        // when - REPORT_BLOCK_THRESHOLD(5)번 신고
        for (int i = 0; i < ReportConstants.REPORT_BLOCK_THRESHOLD; i++) {
            post.incrementReportCountAndBlockIfNeeded();
        }

        // then
        assertThat(post.getReportCount()).isEqualTo(ReportConstants.REPORT_BLOCK_THRESHOLD);
        assertThat(post.getStatus()).isEqualTo(PostStatus.BLOCKED_BY_REPORTS);
    }

    @Test
    @DisplayName("신고 횟수가 threshold - 1일 때는 ACTIVE 상태를 유지한다")
    void incrementReportCount_StaysActiveBeforeThreshold() {
        // given
        Post post = createActivePost();

        // when - threshold - 1번 신고
        for (int i = 0; i < ReportConstants.REPORT_BLOCK_THRESHOLD - 1; i++) {
            post.incrementReportCountAndBlockIfNeeded();
        }

        // then
        assertThat(post.getReportCount()).isEqualTo(ReportConstants.REPORT_BLOCK_THRESHOLD - 1);
        assertThat(post.getStatus()).isEqualTo(PostStatus.ACTIVE);
    }

    @Test
    @DisplayName("ACTIVE가 아닌 상태의 게시글을 신고하면 예외가 발생한다")
    void incrementReportCount_ThrowsExceptionWhenNotActive() {
        // given
        Post post = createPostWithStatus(PostStatus.DELETED_BY_USER);

        // when & then
        assertThatThrownBy(() -> post.incrementReportCountAndBlockIfNeeded())
            .isInstanceOf(ApplicationException.class);
    }

    @Test
    @DisplayName("이미 차단된 게시글을 신고하면 예외가 발생한다")
    void incrementReportCount_ThrowsExceptionWhenAlreadyBlocked() {
        // given
        Post post = createPostWithStatus(PostStatus.BLOCKED_BY_REPORTS);

        // when & then
        assertThatThrownBy(() -> post.incrementReportCountAndBlockIfNeeded())
            .isInstanceOf(ApplicationException.class);
    }

    private Post createActivePost() {
        Post post = Post.createStandalone(
            Location.create(37.5, 127.0),
            "test-s2-token",
            "test content",
            "test-author-id",
            false
        );

        // Post 생성 시 기본 상태가 PENDING이므로 ACTIVE로 변경
        ReflectionTestUtils.setField(post, "status", PostStatus.ACTIVE);

        return post;
    }

    private Post createPostWithStatus(PostStatus status) {
        Post post = createActivePost();
        ReflectionTestUtils.setField(post, "status", status);
        return post;
    }
}
