package com.nowhere.backend.repository;

import com.nowhere.backend.domain.entity.CongestionReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CongestionReportRepository extends JpaRepository<CongestionReport, Long> {

    List<CongestionReport> findByUserId(Long userId);

    List<CongestionReport> findByLocationId(Long locationId);
}
