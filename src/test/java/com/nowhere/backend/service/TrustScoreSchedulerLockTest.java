package com.nowhere.backend.service;

import com.nowhere.backend.repository.CongestionReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 무중단 배포 중 app 2개가 겹칠 때, 다른 인스턴스가 잠금을 잡고 있으면
 * Trust Score 처리를 건너뛰는지 확인한다.
 */
@SpringBootTest
class TrustScoreSchedulerLockTest {

    @Autowired
    private TrustScoreScheduler scheduler;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private CongestionReportRepository reportRepository;

    @Test
    void 다른_인스턴스가_잠금을_잡고_있으면_건너뛰고_풀리면_실행한다() throws Exception {
        when(reportRepository.findByExpiresAtBeforeAndTrustScoreProcessedFalse(any(LocalDateTime.class)))
                .thenReturn(List.of());
        // 앱 기동 직후 스케줄러 자동 실행 기록이 남아 있을 수 있어 지우고 시작한다 (다음 자동 실행은 60초 뒤).
        clearInvocations(reportRepository);

        // 다른 인스턴스 역할: 별도 커넥션에서 세션 단위 잠금을 잡는다.
        try (Connection other = dataSource.getConnection();
             Statement st = other.createStatement()) {
            st.execute("SELECT pg_advisory_lock(" + TrustScoreScheduler.TRUST_SCORE_LOCK_KEY + ")");

            scheduler.processTrustScores();
            verify(reportRepository, never())
                    .findByExpiresAtBeforeAndTrustScoreProcessedFalse(any(LocalDateTime.class));

            st.execute("SELECT pg_advisory_unlock(" + TrustScoreScheduler.TRUST_SCORE_LOCK_KEY + ")");
        }

        scheduler.processTrustScores();
        verify(reportRepository, times(1))
                .findByExpiresAtBeforeAndTrustScoreProcessedFalse(any(LocalDateTime.class));
    }
}
