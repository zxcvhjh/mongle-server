package com.algangi.mongle.post.domain.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.algangi.mongle.comment.domain.model.Comment;
import com.algangi.mongle.global.annotation.ULID;
import com.algangi.mongle.global.entity.TimeBaseEntity;
import com.algangi.mongle.global.exception.ApplicationException;
import com.algangi.mongle.post.exception.PostErrorCode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Entity
@Table(name = "post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Getter
public class Post extends TimeBaseEntity {

    private static final int REPORT_BLOCK_THRESHOLD = 5;

    @Id
    @ULID
    private String id;

    @Embedded
    private Location location;

    @Column(nullable = false)
    private String s2TokenId;

    @Column(nullable = false, length = 2000)
    private String content;

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

    @Column(length = 500)
    private String infoText;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    public static Post createInStaticCloud(
        Location location,
        String s2TokenId,
        String content,
        String authorId,
        Long staticCloudId,
        boolean isAnonymous
    ) {
        return Post.builder()
            .location(location)
            .s2TokenId(s2TokenId)
            .content(content)
            .authorId(authorId)
            .staticCloudId(staticCloudId)
            .isAnonymous(isAnonymous)
            .infoText(null)
            .build();
    }

    public static Post createInDynamicCloud(
        Location location,
        String s2TokenId,
        String content,
        String authorId,
        Long dynamicCloudId,
        boolean isAnonymous
    ) {
        return Post.builder()
            .location(location)
            .s2TokenId(s2TokenId)
            .content(content)
            .authorId(authorId)
            .dynamicCloudId(dynamicCloudId)
            .isAnonymous(isAnonymous)
            .infoText(null)
            .build();
    }

    public static Post createStandalone(
        Location location,
        String s2TokenId,
        String content,
        String authorId,
        boolean isAnonymous
    ) {
        return Post.builder()
            .location(location)
            .s2TokenId(s2TokenId)
            .content(content)
            .authorId(authorId)
            .isAnonymous(isAnonymous)
            .infoText(null)
            .build();
    }

    public static Post createNonExpiredStandalone(
        Location location,
        String s2TokenId,
        String content,
        String authorId,
        boolean isAnonymous,
        String infoText
    ) {
        return Post.builder()
            .location(location)
            .s2TokenId(s2TokenId)
            .content(content)
            .authorId(authorId)
            .isAnonymous(isAnonymous)
            .expiredAt(null)
            .infoText(infoText)
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
        if (content.length() > 2000) {
            throw new IllegalArgumentException("게시물 내용은 2000자를 초과할 수 없습니다.");
        }
        this.content = content;
    }

    public void updateAdminDetails(String content, Boolean isAnonymous, String infoText) {
        if (StringUtils.hasText(content)) {
            if (content.length() > 2000) {
                throw new IllegalArgumentException("게시물 내용은 2000자를 초과할 수 없습니다.");
            }
            this.content = content;
        }
        if (isAnonymous != null) {
            this.isAnonymous = isAnonymous;
        }
        this.infoText = infoText;

        if (this.expiredAt == null) {
            this.expiredAt = null;
        }
        this.status = PostStatus.PENDING;
    }


    public void updatePostFiles(List<PostFile> postFiles) {
        if (postFiles == null) {
            throw new IllegalArgumentException("게시물 파일 목록은 null일 수 없습니다.");
        }
        this.postFiles.clear();
        addPostFiles(postFiles);
    }

    public void updateAnonymity(Boolean isAnonymous) {
        if (isAnonymous != null) {
            this.isAnonymous = isAnonymous;
        }
    }

    public void addPostFiles(List<PostFile> postFiles) {
        postFiles.forEach(this::addPostFile);
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
        if (this.status == PostStatus.PENDING || this.status == PostStatus.ACTIVE) {
            this.status = PostStatus.ACTIVE;
        } else {
        }
    }

    public void markAsExpired() {
        if (this.status == PostStatus.ACTIVE) {
            this.status = PostStatus.EXPIRED;
        }
    }

    public void markAsPending() {
        if (this.status == PostStatus.ACTIVE || this.status == PostStatus.PENDING) {
            this.status = PostStatus.PENDING;
        }
    }

    public void softDeleteByUser() {
        if (isDeletable()) {
            this.status = PostStatus.DELETED_BY_USER;
        }
    }

    public void softDeleteByAdmin() {
        if (isDeletable()) {
            this.status = PostStatus.DELETED_BY_ADMIN;
        }
    }

    private boolean isDeletable() {
        return this.status == PostStatus.ACTIVE ||
            this.status == PostStatus.PENDING ||
            this.status == PostStatus.EXPIRED;
    }


    public void increaseLikeCount(long delta) {
        long newCount = this.likeCount + delta;
        this.likeCount = Math.max(newCount, 0);
    }

    public void increaseDislikeCount(long delta) {
        long newCount = this.dislikeCount + delta;
        this.dislikeCount = Math.max(newCount, 0);
    }

    public void incrementReportCountAndBlockIfNeeded() {
        if (this.status != PostStatus.ACTIVE && this.status != PostStatus.PENDING) {
            throw new ApplicationException(PostErrorCode.INVALID_STATUS);
        }

        this.reportCount = Math.max(0, this.reportCount) + 1;
        if (this.reportCount >= REPORT_BLOCK_THRESHOLD) {
            this.status = PostStatus.BLOCKED_BY_REPORTS;
        }
    }
}

