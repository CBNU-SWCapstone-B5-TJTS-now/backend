package com.nowhere.backend.service;

import com.nowhere.backend.domain.entity.CongestionReport;
import com.nowhere.backend.repository.CongestionReportRepository;
import com.nowhere.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrustScoreScheduler {

    private static final int DISPUTED_THRESHOLD = 3;

    private final CongestionReportRepository reportRepository;
    private final UserRepository userRepository;

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void processTrustScores() {
        List<CongestionReport> expiredReports = reportRepository
                .findByExpiresAtBeforeAndTrustScoreProcessedFalse(LocalDateTime.now());

        if (expiredReports.isEmpty()) return;

        log.info("[TrustScore] 만료 제보 {}건 처리 시작", expiredReports.size());

        for (CongestionReport report : expiredReports) {
            int delta = calculateDelta(report);
            if (delta != 0) {
                report.getUser().addTrustScore(delta);
                userRepository.save(report.getUser());
            }
            report.markTrustScoreProcessed();
            reportRepository.save(report);
        }

        log.info("[TrustScore] 처리 완료");
    }

    private int calculateDelta(CongestionReport report) {
        if (report.getRejectionCount() >= DISPUTED_THRESHOLD) return -1;
        return 0;
    }
}
