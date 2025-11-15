package com.algangi.mongle.post.application.service;

import com.algangi.mongle.comment.application.service.NotifyBotCommentService;
import com.algangi.mongle.global.constants.MessageConstants;
import com.algangi.mongle.post.domain.model.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 게시글 관련 알림 서비스
 * - 게시글 생성 알림 댓글 추가
 */
@Service
@RequiredArgsConstructor
public class PostNotificationService {

    private final NotifyBotCommentService notifyBotCommentService;

    /**
     * 게시글 생성 알림 댓글 추가
     *
     * @param post 생성된 게시글
     * @param currentPostCount 현재 게시글 수
     * @param maxPostCount 최대 게시글 수
     */
    public void addCreationNotification(Post post, long currentPostCount, int maxPostCount) {
        // 현재 게시글 수가 최대치를 초과하지 않도록 조정
        long displayCount = Math.min(currentPostCount + 1, maxPostCount);

        String notifyContent = String.format(
            MessageConstants.POST_CREATION_NOTIFICATION_TEMPLATE,
            displayCount,
            maxPostCount
        );

        notifyBotCommentService.notifyByComment(notifyContent, post);
    }
}
