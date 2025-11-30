package com.algangi.mongle.stats.infrastructure.scheduler;

import com.algangi.mongle.stats.application.service.StatsSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsSyncScheduler {

    private final StatsSyncService statsSyncService;

    @Scheduled(cron = "30 */2 * * * *")
    @SchedulerLock(
            name = "syncCommentsToDb",
            lockAtLeastFor = "PT30S",
            lockAtMostFor = "PT1M55S"
    )
    public void syncComments() {
        try {
            statsSyncService.syncPostCommentCountsToDb();
        } catch (Exception e) {
            log.error("[Scheduler] 댓글 수 동기화 실패", e);
        }
    }

    @Scheduled(cron = "15 */5 * * * *")
    @SchedulerLock(
            name = "syncViewsToDb",
            lockAtLeastFor = "PT1M",
            lockAtMostFor = "PT4M55S"
    )
    public void syncViews() {
        try {
            statsSyncService.syncPostViewCountsToDb();
        } catch (Exception e) {
            log.error("[Scheduler] 조회수 동기화 실패", e);
        }
    }

    @Scheduled(cron = "0 */1 * * * *")
    @SchedulerLock(
            name = "syncReactionsToDb",
            lockAtLeastFor = "PT15S",
            lockAtMostFor = "PT55S"
    )
    public void syncReactions() {
        try {
            statsSyncService.syncReactionCountsToDb();
        } catch (Exception e) {
            log.error("[Scheduler] 반응 수 동기화 실패", e);
        }
    }
}