package com.algangi.mongle.postViewLog.domain.repository;

import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.postViewLog.domain.model.PostViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostViewLogRepository extends JpaRepository<PostViewLog, String> {
    boolean existsByMemberAndPost(Member member, Post post);
}
