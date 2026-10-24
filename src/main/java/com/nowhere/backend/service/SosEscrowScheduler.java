package com.nowhere.backend.service;

import com.nowhere.backend.domain.entity.PointTransactionType;
import com.nowhere.backend.domain.entity.SosPostStatus;
import com.nowhere.backend.repository.SosPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SOS 포인트 에스크로 자동 취소: 답변 수락 후 60분 내 완료되지 않은 거래를
 * 취소하고 질문자에게 포인트를 환불한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SosEscrowScheduler {

    /**
     * 무중단 배포 중 app 2개가 겹쳐도 같은 거래를 두 번 환불하지 않도록
     * PostgreSQL advisory lock으로 한 인스턴스만 실행한다. (TrustScoreScheduler와 다른 키)
     */
    static final long SOS_ESCROW_LOCK_KEY = 7_201_002L;

    private final SosPostRepository sosPostRepository;
    private final SosService sosService;
    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelay = 60000) // 1분마다 실행
    @Transactional
    public void refundTimedOutEscrows() {
        Boolean locked = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_xact_lock(?)", Boolean.class, SOS_ESCROW_LOCK_KEY);
        if (!Boolean.TRUE.equals(locked)) {
            log.debug("[SosEscrow] 다른 인스턴스가 처리 중이라 건너뜀");
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(SosRedisService.ESCROW_TTL_MIN);
        List<Long> postIds = sosPostRepository.findEscrowTimedOutPostIds(
                SosPostStatus.ACCEPTED, PointTransactionType.SOS_ESCROW, cutoff);

        if (postIds.isEmpty()) return;

        int cancelled = 0;
        for (Long postId : postIds) {
            if (sosService.autoCancelTimedOutEscrow(postId)) cancelled++;
        }
        log.info("[SosEscrow] 수락 후 60분 경과 거래 {}건 자동 취소·환불", cancelled);
    }
}
