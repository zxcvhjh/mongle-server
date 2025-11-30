package com.algangi.mongle.post.event;

import com.algangi.mongle.stats.application.service.ContentStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PostDeletedEventListener {

    private final ContentStatsService contentStatsService;

    @Async("statsUpdateTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostDeletion(PostDeletedEvent event) {
        contentStatsService.cleanupStatsForDeletedPost(event.postId());
    }
}