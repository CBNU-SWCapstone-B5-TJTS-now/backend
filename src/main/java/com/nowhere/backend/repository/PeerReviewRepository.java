package com.nowhere.backend.repository;

import com.nowhere.backend.domain.entity.PeerReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeerReviewRepository extends JpaRepository<PeerReview, Long> {

    boolean existsByReportIdAndReviewerId(Long reportId, Long reviewerId);
}
