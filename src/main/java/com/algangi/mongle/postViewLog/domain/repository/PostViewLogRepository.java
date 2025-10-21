package com.algangi.mongle.postViewLog.domain.repository;

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

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
            value = "INSERT INTO post_view_log (id, member_id, post_id, created_date) " +
                    "VALUES (:id, :memberId, :postId, :createdDate) " +
                    "ON DUPLICATE KEY UPDATE id = id",
            nativeQuery = true
    )
    void insertIfNotExists(
            @Param("id") String id,
            @Param("memberId") String memberId,
            @Param("postId") String postId,
            @Param("createdDate") Instant createdDate
    );
}