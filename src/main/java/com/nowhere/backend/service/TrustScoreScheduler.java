package com.nowhere.backend.service;

import com.nowhere.backend.domain.entity.CongestionReport;
import com.nowhere.backend.repository.CongestionReportRepository;
import com.nowhere.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
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

    /**
     * 무중단 배포 중에는 app 2개가 잠시 함께 떠 있으므로, 같은 제보를 두 번 처리해
     * 점수가 중복 차감되지 않도록 PostgreSQL advisory lock으로 한 인스턴스만 실행한다.
     * 트랜잭션 단위 잠금이라 커밋/롤백 시 자동으로 풀린다.
     */
    static final long TRUST_SCORE_LOCK_KEY = 7_201_001L;

    private final CongestionReportRepository reportRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void processTrustScores() {
        Boolean locked = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_xact_lock(?)", Boolean.class, TRUST_SCORE_LOCK_KEY);
        if (!Boolean.TRUE.equals(locked)) {
            log.debug("[TrustScore] 다른 인스턴스가 처리 중이라 건너뜀");
            return;
        }

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
