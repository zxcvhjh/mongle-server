package com.algangi.mongle.post.event;

import com.algangi.mongle.postViewLog.domain.repository.PostViewLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberViewedPostEventListener {

    private final PostViewLogRepository postViewLogRepository;

    /*@Async("persistenceTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMemberViewedPost(MemberViewedPostEvent event) {
        try {
            postViewLogRepository.insertIfNotExists(
                    com.github.f4b6a3.ulid.UlidCreator.getUlid().toString(),
                    event.memberId(),
                    event.postId(),
                    java.time.Instant.now()
            );
        } catch (Exception e) {
            log.error("게시물 조회 기록 저장 중 DB 오류 발생. MemberId={}, PostId={}", event.memberId(), event.postId(), e);
        }
    }*/
}
