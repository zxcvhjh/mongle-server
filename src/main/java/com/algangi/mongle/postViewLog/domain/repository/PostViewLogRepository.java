package com.algangi.mongle.postViewLog.domain.repository;

import com.algangi.mongle.member.domain.model.Member;
import com.algangi.mongle.post.domain.model.Post;
import com.algangi.mongle.postViewLog.domain.model.PostViewLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

public interface PostViewLogRepository extends JpaRepository<PostViewLog, String> {
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM PostViewLog pvl WHERE pvl.member.memberId = :memberId")
    void deleteAllByMemberId(@Param("memberId") String memberId);

    @Query("SELECT pvl FROM PostViewLog pvl JOIN FETCH pvl.member JOIN FETCH pvl.post WHERE pvl.createdDate > :since")
    Stream<PostViewLog> streamAllByCreatedDateAfterWithJoins(@Param("since") Instant since);

    @EntityGraph(attributePaths = {"member", "post"})
    List<PostViewLog> findByMember_MemberIdAndCreatedDateAfter(String memberId, Instant since);
}