package com.algangi.mongle.comment.application.service;

import com.algangi.mongle.comment.domain.model.Comment;
import com.algangi.mongle.comment.domain.repository.CommentRepository;
import com.algangi.mongle.comment.domain.service.CommentDomainService;
import com.algangi.mongle.comment.domain.service.CommentFinder;
import com.algangi.mongle.comment.exception.CommentErrorCode;
import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.member.domain.model.MemberRole;
import com.algangi.mongle.member.domain.model.MemberStatus;
import com.algangi.mongle.post.application.helper.PostFinder;
import com.algangi.mongle.post.domain.model.Location;
import com.algangi.mongle.post.domain.model.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CommentCommandService 테스트
 * NPE 및 권한 검증 로직 테스트
 */
@ExtendWith(MockitoExtension.class)
class CommentCommandServiceTest {

    @Mock
    private MemberFinder memberFinder;

    @Mock
    private PostFinder postFinder;

    @Mock
    private CommentFinder commentFinder;

    @Mock
    private CommentDomainService commentDomainService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CommentCommandService commentCommandService;

    private Member normalMember;
    private Member adminMember;
    private Post testPost;
    private Comment normalComment;
    private Comment anonymousComment;

    @BeforeEach
    void setUp() {
        // 일반 사용자 생성
        normalMember = createMember("user-id", "testUser", MemberRole.USER, MemberStatus.ACTIVE);

        // 관리자 생성
        adminMember = createMember("admin-id", "adminUser", MemberRole.ADMIN, MemberStatus.ACTIVE);

        // 테스트용 게시글 생성
        testPost = Post.createStandalone(
            Location.create(37.5, 127.0),
            "test-s2-token",
            "test post content",
            "author-id",
            false
        );

        // 일반 댓글 생성 (member 설정됨)
        normalComment = Comment.createParentComment(
            "normal comment content",
            testPost,
            normalMember,
            false
        );
        ReflectionTestUtils.setField(normalComment, "id", "comment-id-1");

        // 익명 댓글 생성 (member가 null)
        anonymousComment = Comment.createParentComment(
            "anonymous comment content",
            testPost,
            null,
            true
        );
        ReflectionTestUtils.setField(anonymousComment, "id", "comment-id-2");
    }

    @Test
    @DisplayName("익명 댓글을 일반 사용자가 삭제 시도하면 권한 없음 예외 발생 (NPE 발생하지 않음)")
    void deleteAnonymousComment_ByNormalUser_ThrowsAccessDenied_NotNPE() {
        // given
        when(memberFinder.getMemberOrThrow("user-id")).thenReturn(normalMember);
        when(commentFinder.getCommentOrThrow("comment-id-2")).thenReturn(anonymousComment);

        // when & then
        // NPE가 발생하지 않고 COMMENT_ACCESS_DENIED 예외가 발생해야 함
        assertThatThrownBy(() -> commentCommandService.deleteComment("comment-id-2", "user-id"))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_ACCESS_DENIED);

        // deleteComment가 호출되지 않아야 함
        verify(commentDomainService, never()).deleteComment(any());
    }

    @Test
    @DisplayName("익명 댓글을 관리자가 삭제하면 성공")
    void deleteAnonymousComment_ByAdmin_Success() {
        // given
        when(memberFinder.getMemberOrThrow("admin-id")).thenReturn(adminMember);
        when(commentFinder.getCommentOrThrow("comment-id-2")).thenReturn(anonymousComment);

        // when
        commentCommandService.deleteComment("comment-id-2", "admin-id");

        // then
        verify(commentDomainService, times(1)).deleteComment(anonymousComment);
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    @DisplayName("일반 댓글을 작성자가 삭제하면 성공")
    void deleteNormalComment_ByAuthor_Success() {
        // given
        when(memberFinder.getMemberOrThrow("user-id")).thenReturn(normalMember);
        when(commentFinder.getCommentOrThrow("comment-id-1")).thenReturn(normalComment);

        // when
        commentCommandService.deleteComment("comment-id-1", "user-id");

        // then
        verify(commentDomainService, times(1)).deleteComment(normalComment);
        verify(eventPublisher, times(1)).publishEvent(any());
    }

    @Test
    @DisplayName("일반 댓글을 다른 사용자가 삭제 시도하면 권한 없음 예외 발생")
    void deleteNormalComment_ByOtherUser_ThrowsAccessDenied() {
        // given
        Member otherMember = createMember("other-user-id", "otherUser", MemberRole.USER, MemberStatus.ACTIVE);
        when(memberFinder.getMemberOrThrow("other-user-id")).thenReturn(otherMember);
        when(commentFinder.getCommentOrThrow("comment-id-1")).thenReturn(normalComment);

        // when & then
        assertThatThrownBy(() -> commentCommandService.deleteComment("comment-id-1", "other-user-id"))
            .isInstanceOf(ApplicationException.class)
            .hasFieldOrPropertyWithValue("errorCode", CommentErrorCode.COMMENT_ACCESS_DENIED);

        verify(commentDomainService, never()).deleteComment(any());
    }

    private Member createMember(String memberId, String nickname, MemberRole role, MemberStatus status) {
        Member member = Member.createMember(
            memberId,
            nickname,
            "test@example.com",
            "encodedPassword",
            role
        );
        ReflectionTestUtils.setField(member, "status", status);
        return member;
    }
}
