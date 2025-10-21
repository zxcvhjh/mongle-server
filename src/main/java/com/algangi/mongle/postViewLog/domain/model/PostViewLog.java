package com.algangi.mongle.postViewLog.domain.model;

import com.algangi.mongle.global.annotation.ULID;
import com.algangi.mongle.global.entity.CreatedDateBaseEntity;
import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.domain.model.Post;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_view_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@Getter
public class PostViewLog extends CreatedDateBaseEntity {

    @Id
    @ULID
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    private PostViewLog(Member member, Post post) {
        this.member = member;
        this.post = post;
    }

    public static PostViewLog of(Member member, Post post) {
        return new PostViewLog(member, post);
    }
}