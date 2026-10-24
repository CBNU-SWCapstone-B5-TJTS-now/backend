package com.nowhere.backend.repository;

import com.nowhere.backend.domain.entity.SosPost;
import com.nowhere.backend.domain.entity.SosPostStatus;
import com.nowhere.backend.domain.entity.PointTransactionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SosPostRepository extends JpaRepository<SosPost, Long> {

    /** 완료·취소·자동 환불이 같은 게시글을 동시에 처리하지 않도록 행 잠금으로 조회 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from SosPost p where p.id = :id")
    Optional<SosPost> findByIdForUpdate(@Param("id") Long id);

    /** 수락(에스크로 보관) 시각이 cutoff 이전인데 아직 완료·취소되지 않은 게시글 ID */
    @Query("select p.id from SosPost p where p.status = :status and p.pointOffer > 0 "
            + "and exists (select t.id from PointTransaction t "
            + "where t.sosPostId = p.id and t.type = :escrowType and t.createdAt < :cutoff)")
    List<Long> findEscrowTimedOutPostIds(@Param("status") SosPostStatus status,
                                         @Param("escrowType") PointTransactionType escrowType,
                                         @Param("cutoff") LocalDateTime cutoff);

    List<SosPost> findByLocationIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            Long locationId, SosPostStatus status, LocalDateTime now);

    List<SosPost> findByStatusAndExpiresAtBefore(SosPostStatus status, LocalDateTime now);
}
