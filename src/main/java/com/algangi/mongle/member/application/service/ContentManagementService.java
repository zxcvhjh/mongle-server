package com.algangi.mongle.member.application.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.algangi.mongle.comment.domain.model.Comment;
import com.algangi.mongle.comment.domain.repository.CommentRepository;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.post.domain.model.PostStatus;
import com.algangi.mongle.post.domain.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentManagementService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final ContentManagementDbService dbService;
    private final RedisTemplate<String, String> redisTemplate;

    // 신고 기능 개편으로 인해, 사용자 단위 제재 로직(ContentManagementService.processCommentsOfBannedUser, processPostsOfBannedUser)은 제거되었습니다.

    private List<Comment> findCommentsByBannedUser(String bannedMemberId) {
        return commentRepository.findAllByMemberIdAndPostStatusIn(
            bannedMemberId, List.of(PostStatus.PENDING, PostStatus.ACTIVE)
        );
    }

    public void cleanupRedisDataForComments(List<String> commentIds,
        Map<String, Long> postCommentCountDelta, Map<String, List<String>> commentsByPost) {
        var serializer = redisTemplate.getStringSerializer();

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            // 1. 댓글 관련 키 일괄 삭제
            if (!commentIds.isEmpty()) {
                byte[][] keysToDelete = commentIds.stream()
                    .flatMap(commentId -> Stream.of(
                        serializer.serialize("likes::comment::" + commentId),
                        serializer.serialize("dislikes::comment::" + commentId),
                        serializer.serialize("reactions::comment::" + commentId)
                    ))
                    .toArray(byte[][]::new);
                connection.keyCommands().del(keysToDelete);
            }

            // 2. 게시글별 댓글 수 감소
            postCommentCountDelta.forEach((postId, count) -> {
                byte[] commentCountKey = serializer.serialize("comments::post::" + postId);
                connection.stringCommands().decrBy(commentCountKey, count);
            });

            // 3. 댓글 랭킹 정리
            commentsByPost.forEach((postId, ids) -> {
                if (!ids.isEmpty()) {
                    byte[] rankingKey = serializer.serialize("comments_by_likes::post::" + postId);
                    byte[][] membersToDelete = ids.stream()
                        .map(serializer::serialize)
                        .toArray(byte[][]::new);
                    connection.zSetCommands().zRem(rankingKey, membersToDelete);
                }
            });

            return null;
        });
    }

    public void cleanupRedisDataForPosts(List<String> postIds) {
        var serializer = redisTemplate.getStringSerializer();

        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            if (!postIds.isEmpty()) {
                byte[][] keysToDelete = postIds.stream()
                    .flatMap(postId -> Stream.of(
                        serializer.serialize("views::post::" + postId),
                        serializer.serialize("comments::post::" + postId),
                        serializer.serialize("likes::post::" + postId),
                        serializer.serialize("dislikes::post::" + postId),
                        serializer.serialize("reactions::post::" + postId),
                        serializer.serialize("comments_by_likes::post::" + postId)
                    ))
                    .toArray(byte[][]::new);
                connection.keyCommands().del(keysToDelete);
            }
            return null;
        });
    }
}
