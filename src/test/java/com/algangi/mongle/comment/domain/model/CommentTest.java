package com.algangi.mongle.comment.domain.model;

import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.post.domain.model.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentTest {

    private Post mockPost = Post.createStandalone(Location.create(37.5, 127.0), "s2", "p-content",
        "author-id", false);

    private Comment createCommentWithStatus(CommentStatus status) {
        Comment comment = Comment.createParentComment("test content", mockPost, null, false);

        org.springframework.test.util.ReflectionTestUtils.setField(comment, "status", status);
        return comment;
    }

    @ParameterizedTest
    @EnumSource(value = CommentStatus.class, names = {
        "DELETED_BY_USER",
        "DELETED_BY_ADMIN",
        "DELETED_BY_WITHDRAWAL",
        "BLOCKED_BY_REPORTS"
    })
    @DisplayName("삭제/차단 상태는 isDeleted()가 True를 반환해야 한다")
    void isDeleted_ReturnsTrueForDeletedAndBlocked(CommentStatus status) {
        Comment comment = createCommentWithStatus(status);
        assertTrue(comment.isDeleted());
    }

    @Test
    @DisplayName("ACTIVE 상태는 isDeleted()가 False를 반환해야 한다")
    void isDeleted_ReturnsFalseForActive() {
        Comment comment = createCommentWithStatus(CommentStatus.ACTIVE);
        assertFalse(comment.isDeleted());
    }
}