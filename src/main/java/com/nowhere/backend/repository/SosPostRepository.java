package com.nowhere.backend.repository;

import com.nowhere.backend.domain.entity.SosPost;
import com.nowhere.backend.domain.entity.SosPostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SosPostRepository extends JpaRepository<SosPost, Long> {

    List<SosPost> findByLocationIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            Long locationId, SosPostStatus status, LocalDateTime now);

    List<SosPost> findByStatusAndExpiresAtBefore(SosPostStatus status, LocalDateTime now);
}
