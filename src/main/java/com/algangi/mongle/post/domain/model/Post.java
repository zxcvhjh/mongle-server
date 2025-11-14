package com.algangi.mongle.post.domain.model;

import com.algangi.mongle.comment.domain.model.Comment;
import com.algangi.mongle.global.annotation.ULID;
import com.algangi.mongle.global.constants.ReportConstants;
import com.algangi.mongle.global.entity.TimeBaseEntity;
import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.post.exception.PostErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE, toBuilder = true)
@Getter
public class Post extends TimeBaseEntity {

    @Id
    @ULID
    private String id;

    @Embedded
    private Location location;

    @Column(nullable = false)
    private String s2TokenId;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(length = 2000)
    private String infoText;

    @Column(nullable = false)
    @Builder.Default
    private long viewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long commentCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long dislikeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private long reportCount = 0;

    @Version
    private Long version;

    @Builder.Default
    private Double rankingScore = 0.0;

    @Column(nullable = true)
    @Builder.Default
    private Instant expiredAt = Instant.now().plus(24, ChronoUnit.HOURS);

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PostStatus status = PostStatus.PENDING;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PostFile> postFiles = new ArrayList<>();

    @Column(nullable = true)
    private String authorId;

    @Column(nullable = false)
    @Builder.Default
    private boolean isAnonymous = false;

    private Long dynamicCloudId;

    private Long staticCloudId;

    @Column(length = 255)
    private String customNickname;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();


    public static Post createInStaticCloud(
        Location location, String s2TokenId, String content, String authorId,
        Long staticCloudId, boolean isAnonymous) {
        return Post.builder()
            .location(location)
            .s2TokenId(s2TokenId)
            .content(content)
            .authorId(authorId)
            .staticCloudId(staticCloudId)
            .isAnonymous(isAnonymous)
            .build();
    }

    public static Post createInDynamicCloud(
        Location location, String s2TokenId, String content, String authorId,
        Long dynamicCloudId, boolean isAnonymous) {
        return Post.builder()
            .location(location)
            .s2TokenId(s2TokenId)
            .content(content)
            .authorId(authorId)
            .dynamicCloudId(dynamicCloudId)
            .isAnonymous(isAnonymous)
            .build();
    }

    public static Post createStandalone(
        Location location, String s2TokenId, String content, String authorId,
        boolean isAnonymous) {
        return Post.builder()
            .location(location)
            .s2TokenId(s2TokenId)
            .content(content)
            .authorId(authorId)
            .isAnonymous(isAnonymous)
            .build();
    }

    public static Post createNonExpiredStandalone(
        Location location, String s2TokenId, String content, String authorId,
        boolean isAnonymous, String infoText, String customNickname) {
        return Post.builder()
            .location(location)
            .s2TokenId(s2TokenId)
            .content(content)
            .authorId(authorId)
            .isAnonymous(isAnonymous)
            .infoText(infoText)
            .customNickname(customNickname)
            .expiredAt(null)
            .build();
    }


    public void assignToDynamicCloud(Long dynamicCloudId) {
        this.dynamicCloudId = dynamicCloudId;
        this.staticCloudId = null;
    }

    public void updateContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("게시물 내용은 비워둘 수 없습니다.");
        }
        this.content = content;
    }

    public void updateAdminDetails(String content, Boolean isAnonymous, String infoText,
        String customNickname) {
        if (content != null) {
            updateContent(content);
        }
        if (isAnonymous != null) {
            this.isAnonymous = isAnonymous;
        }
        this.infoText = infoText;
        this.customNickname = StringUtils.hasText(customNickname) ? customNickname : null;
    }

    public void updateInfoText(String newInfoText) {
        this.infoText = newInfoText;
    }


    public void updatePostFiles(List<PostFile> newPostFiles) {
        this.postFiles.clear();
        addPostFiles(newPostFiles);
    }

    public void updateAnonymity(Boolean isAnonymous) {
        if (isAnonymous != null) {
            this.isAnonymous = isAnonymous;
        }
    }

    public void addPostFiles(List<PostFile> postFilesToAdd) {
        if (postFilesToAdd != null) {
            postFilesToAdd.forEach(this::addPostFile);
        }
    }

    public void addPostFile(PostFile postFile) {
        this.postFiles.add(postFile);
        postFile.setPost(this);
    }

    public void addComment(Comment comment) {
        this.comments.add(comment);
        comment.setPost(this);
    }


    public void markAsActive() {
        this.status = PostStatus.ACTIVE;
    }

    public void markAsExpired() {
        this.status = PostStatus.EXPIRED;
    }

    public void markAsPending() {
        this.status = PostStatus.PENDING;
    }

    public void softDeleteByUser() {
        if (isAlreadyDeletedOrBlocked()) {
            return;
        }
        this.status = PostStatus.DELETED_BY_USER;
    }

    public void softDeleteByAdmin() {
        if (isAlreadyDeletedOrBlocked()) {
            return;
        }
        this.status = PostStatus.DELETED_BY_ADMIN;
    }


    public void increaseLikeCount(long delta) {
        this.likeCount = Math.max(0, this.likeCount + delta);
    }

    public void increaseDislikeCount(long delta) {
        this.dislikeCount = Math.max(0, this.dislikeCount + delta);
    }

    public void incrementReportCountAndBlockIfNeeded() {
        if (this.status != PostStatus.ACTIVE) {
            throw new ApplicationException(PostErrorCode.INVALID_STATUS);
        }
        this.reportCount++;
        if (this.reportCount >= ReportConstants.REPORT_BLOCK_THRESHOLD) {
            this.status = PostStatus.BLOCKED_BY_REPORTS;
        }
    }


    private boolean isAlreadyDeletedOrBlocked() {
        return this.status == PostStatus.DELETED_BY_USER ||
            this.status == PostStatus.DELETED_BY_ADMIN ||
            this.status == PostStatus.DELETED_BY_WITHDRAWAL ||
            this.status == PostStatus.BLOCKED_BY_REPORTS;
    }
}

