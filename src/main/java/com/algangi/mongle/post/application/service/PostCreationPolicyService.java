package com.algangi.mongle.post.application.service;

import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.member.domain.model.MemberRole;
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
     * Member 도메인의 validateActive() 메서드 위임
     */
    public void validateMemberStatus(Member member) {
        member.validateActive();
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
     * 부스는 최대 1개의 게시글만 생성 가능
     */
    public void validateBoothPostLimit(String authorId) {
        long activePostCount = postRepository.countByAuthorIdAndStatus(authorId, PostStatus.ACTIVE);
        // activePostCount >= 1이면 이미 1개 존재하므로 추가 생성 불가
        if (activePostCount >= MAX_BOOTH_POST_COUNT) {
            throw new ApplicationException(PostErrorCode.BOOTH_POST_MAXIMUM_EXCEED);
        }
    }

    /**
     * 일반 사용자의 게시글 수 제한 체크 및 초과 시 가장 오래된 게시글 만료 처리
     * 관리자는 제한 없음
     *
     * NOTE: 동시성 제어는 Member에 대한 Pessimistic Lock으로 보장됨
     * (PostCreationService에서 getMemberWithLockOrThrow 호출)
     * 따라서 동일 사용자의 게시글 생성 요청은 직렬화되어 race condition이 발생하지 않음
     *
     * @return 현재 활성 게시글 수
     */
    public long checkAndHandlePostCountLimit(Member member) {
        if (member.isAdmin()) {
            return 0;
        }

        long existingPostCount = postRepository.countByAuthorIdAndStatus(
            member.getMemberId(), PostStatus.ACTIVE);

        // existingPostCount가 5 이상이면 oldest 만료 후 새 게시글 생성
        // 결과: 항상 최대 5개 유지
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
