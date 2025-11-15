package com.algangi.mongle.post.application.service;

import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.member.domain.model.MemberRole;
import com.algangi.mongle.member.domain.model.MemberStatus;
import com.algangi.mongle.member.exception.MemberErrorCode;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.post.domain.model.PostStatus;
import com.algangi.mongle.post.domain.repository.PostRepository;
import com.algangi.mongle.post.exception.PostErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 게시글 생성 정책 검증 서비스
 * - Rate limiting
 * - 게시글 수 제한
 * - 사용자 상태 검증
 */
@Service
@RequiredArgsConstructor
public class PostCreationPolicyService {

    private static final int MAX_POST_COUNT_PER_USER = 5;
    private static final int MAX_BOOTH_POST_COUNT = 1;

    private final PostRepository postRepository;
    private final PostRateLimiter postRateLimiter;

    /**
     * 사용자 상태가 활성화되어 있는지 검증
     */
    public void validateMemberStatus(Member member) {
        if (member.getStatus() == MemberStatus.BANNED) {
            throw new ApplicationException(MemberErrorCode.MEMBER_IS_BANNED);
        }
        if (member.getStatus() == MemberStatus.DEACTIVATED) {
            throw new ApplicationException(MemberErrorCode.MEMBER_IS_DEACTIVATED);
        }
    }

    /**
     * Rate limiting 체크 (관리자는 제외)
     */
    public void checkRateLimit(Member member) {
        if (!member.isAdmin()) {
            postRateLimiter.checkRateLimit(member.getMemberId());
        }
    }

    /**
     * Rate limiting 블록 적용 (관리자는 제외)
     */
    public void applyRateLimitBlock(Member member) {
        if (!member.isAdmin()) {
            postRateLimiter.blockUser(member.getMemberId());
        }
    }

    /**
     * 부스 계정의 게시글 수 제한 체크
     */
    public void validateBoothPostLimit(String authorId) {
        long activePostCount = postRepository.countByAuthorIdAndStatus(authorId, PostStatus.ACTIVE);
        if (activePostCount >= MAX_BOOTH_POST_COUNT) {
            throw new ApplicationException(PostErrorCode.BOOTH_POST_MAXIMUM_EXCEED);
        }
    }

    /**
     * 일반 사용자의 게시글 수 제한 체크 및 초과 시 가장 오래된 게시글 만료 처리
     * 관리자는 제한 없음
     *
     * @return 현재 활성 게시글 수
     */
    public long checkAndHandlePostCountLimit(Member member) {
        if (member.isAdmin()) {
            return 0;
        }

        long existingPostCount = postRepository.countByAuthorIdAndStatus(
            member.getMemberId(), PostStatus.ACTIVE);

        if (existingPostCount >= MAX_POST_COUNT_PER_USER) {
            Optional<Post> oldestPost = postRepository.findFirstByAuthorIdAndStatusOrderByCreatedDateAsc(
                member.getMemberId(), PostStatus.ACTIVE);
            oldestPost.ifPresent(Post::markAsExpired);
        }

        return existingPostCount;
    }

    /**
     * 사용자가 알림을 받아야 하는지 판단
     * 관리자와 부스는 알림 제외
     */
    public boolean shouldReceiveNotification(Member member) {
        return member.getMemberRole() == MemberRole.USER;
    }

    /**
     * 최대 게시글 수 반환
     */
    public int getMaxPostCountPerUser() {
        return MAX_POST_COUNT_PER_USER;
    }
}
