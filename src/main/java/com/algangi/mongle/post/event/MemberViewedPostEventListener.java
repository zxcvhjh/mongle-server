package com.algangi.mongle.post.event;

import com.algangi.mongle.member.application.service.MemberFinder;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.application.helper.PostFinder;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.postViewLog.domain.model.PostViewLog;
import com.algangi.mongle.postViewLog.domain.repository.PostViewLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberViewedPostEventListener {

    private final MemberFinder memberFinder;
    private final PostFinder postFinder;
    private final PostViewLogRepository postViewLogRepository;

    @Async("persistenceTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberViewedPost(MemberViewedPostEvent event) {
        try {
            Member member = memberFinder.getMemberOrThrow(event.memberId());
            Post post = postFinder.getPostOrThrow(event.postId());

            if (postViewLogRepository.existsByMemberAndPost(member, post)) {
                log.info("이미 조회 기록이 존재하여 중복 저장을 방지했습니다. MemberId={}, PostId={}", event.memberId(), event.postId());
                return;
            }

            PostViewLog postViewLog = PostViewLog.of(member, post);
            postViewLogRepository.save(postViewLog);

            log.info("게시물 조회 기록 비동기 저장 완료: MemberId={}, PostId={}", event.memberId(), event.postId());

        } catch (DataIntegrityViolationException e) {
            log.warn("게시물 조회 기록 저장 중 UNIQUE 제약조건 위반 발생. (동시성 이슈 추정) MemberId={}, PostId={}", event.memberId(), event.postId());
        } catch (Exception e) {
            log.error("게시물 조회 기록 저장 중 예상치 못한 오류 발생. MemberId={}, PostId={}", event.memberId(), event.postId(), e);
        }
    }
}
