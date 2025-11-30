package com.algangi.mongle.member.application.service;

import java.util.List;

import com.algangi.mongle.postViewLog.application.service.PostViewLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.algangi.mongle.auth.application.service.authentication.LogoutService;
import com.algangi.mongle.comment.domain.model.Comment;
import com.algangi.mongle.comment.domain.repository.CommentRepository;
import com.algangi.mongle.file.application.service.FileService;
import com.algangi.mongle.member.application.event.MemberWithdrawnEvent;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.member.domain.repository.MemberRepository;
import com.algangi.mongle.post.domain.repository.PostRepository;
import com.algangi.mongle.reaction.domain.repository.ReactionRepository;
import com.algangi.mongle.reaction.infrastructure.persistence.ReactionRepositoryCustom;
import com.algangi.mongle.stats.application.dto.ReactionCleanupDto;
import com.algangi.mongle.stats.application.service.ContentStatsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WithdrawalCleanupService {

    private final ContentStatsService contentStatsService;
    private final MemberRepository memberRepository;
    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepositoryCustom reactionRepositoryCustom;
    private final ContentManagementDbService dbService;
    private final LogoutService logoutService;
    private final FileService fileService;
    private final MemberFinder memberFinder;
    private final PostViewLogService postViewLogService;

    @Transactional
    public void cleanupDataFor(MemberWithdrawnEvent event) {
        String memberId = event.memberId();

        // 사용자 프로필 삭제
        Member member = memberFinder.getMemberOrThrow(memberId);
        if (member.getProfileImage() != null) {
            fileService.deletePermanentFiles(List.of(member.getProfileImage()));
        }

        // 소셜 계정 연동 삭제
        //orphanRemoval=true에 의해 Member 엔티티 삭제 시 같이 삭제됨

        // 리프레쉬 토큰 무효화 처리(로그아웃)
        logoutService.logout(memberId);

        // 리액션 정리
        List<ReactionCleanupDto> reactions = reactionRepositoryCustom.findAllReactionCleanupData(memberId);
        contentStatsService.removeReactionsFromRedis(memberId, reactions);
        reactionRepository.deleteAllByMemberId(memberId);

        // 게시물 조회 기록 정리
        postViewLogService.cleanupViewLogs(memberId);

        // 댓글, 게시글 정리
        List<Comment> comments = commentRepository.findAllByMember_MemberId(memberId);
        List<String> commentIds = comments.stream().map(Comment::getId).toList();
        for (Comment comment : comments) {
            contentStatsService.cleanupStatsForDeletedComment(comment.getId(), comment.getPost().getId());
        }

        List<String> postIds = postRepository.findAllIdsByMemberId(memberId);
        for (String postId : postIds) {
            contentStatsService.cleanupStatsForDeletedPost(postId);
        }

        dbService.updateWithdrawnUserCommentsInDb(commentIds);
        dbService.updateWithdrawnUserPostsInDb(postIds);

        postRepository.unlinkMemberFromPosts(memberId);
        commentRepository.unlinkMemberFromComments(memberId);

        // 멤버 엔티티 삭제
        memberRepository.deleteById(memberId);
    }
}