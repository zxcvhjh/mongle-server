package com.algangi.mongle.postViewLog.domain.repository;

import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.postViewLog.domain.model.PostViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PostViewLogRepository extends JpaRepository<PostViewLog, String> {
    boolean existsByMemberAndPost(Member member, Post post);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM PostViewLog pvl WHERE pvl.member.memberId = :memberId")
    void deleteAllByMemberId(@Param("memberId") String memberId);
}
