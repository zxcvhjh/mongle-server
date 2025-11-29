package com.algangi.mongle.post.infrastructure.scheduler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.algangi.mongle.post.domain.repository.PostRepository;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostExpirationScheduler {

    private final PostRepository postRepository;

    //@Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(
        name = "deleteExpiredPostsLock",
        lockAtLeastFor = "PT5M",
        lockAtMostFor = "PT15M"
    )
    @Transactional
    public void deleteExpiredPosts() {
        log.info("[ShedLock] 만료된 게시글 삭제 작업 시작.");
        Instant now = Instant.now();

        int expiredPostCount = postRepository.expirePosts(now);

        log.info("[ShedLock] {}개의 만료된 게시글을 삭제했습니다.", expiredPostCount);
    }
}
