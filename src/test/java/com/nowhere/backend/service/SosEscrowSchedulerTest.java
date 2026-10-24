package com.nowhere.backend.service;

import com.nowhere.backend.domain.entity.*;
import com.nowhere.backend.repository.PointTransactionRepository;
import com.nowhere.backend.repository.SosPostRepository;
import com.nowhere.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 PostgreSQL에서 수락 후 60분이 지난 에스크로 거래가 자동 취소·환불되는지 확인한다.
 * 테스트 트랜잭션은 끝나면 롤백된다.
 */
@SpringBootTest
@Transactional
class SosEscrowSchedulerTest {

    @Autowired private SosEscrowScheduler scheduler;
    @Autowired private UserRepository userRepository;
    @Autowired private SosPostRepository sosPostRepository;
    @Autowired private PointTransactionRepository pointTransactionRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private EntityManager entityManager;

    @Test
    void 수락_후_60분이_지난_거래만_취소하고_질문자에게_환불한다() {
        User asker = saveUser(150);
        SosPost timedOut = saveAcceptedPost(asker, 100);
        SosPost recent = saveAcceptedPost(asker, 50);
        entityManager.flush();

        // timedOut 거래만 에스크로 보관 시각을 61분 전으로 되돌린다
        jdbcTemplate.update(
                "UPDATE point_transactions SET created_at = ? WHERE sos_post_id = ?",
                LocalDateTime.now().minusMinutes(61), timedOut.getId());
        entityManager.clear();

        scheduler.refundTimedOutEscrows();
        entityManager.flush();
        entityManager.clear();

        assertThat(sosPostRepository.findById(timedOut.getId()).orElseThrow().getStatus())
                .isEqualTo(SosPostStatus.CANCELLED);
        assertThat(sosPostRepository.findById(recent.getId()).orElseThrow().getStatus())
                .isEqualTo(SosPostStatus.ACCEPTED);
        // 수락 시 150P 차감 → timedOut 100P만 환불
        assertThat(userRepository.findById(asker.getId()).orElseThrow().getPoint()).isEqualTo(100);
        assertThat(pointTransactionRepository.findByUserOrderByCreatedAtDesc(asker))
                .anyMatch(t -> t.getType() == PointTransactionType.SOS_REFUND
                        && t.getAmount() == 100 && t.getSosPostId().equals(timedOut.getId()));
    }

    private User saveUser(int point) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return userRepository.save(User.builder()
                .email("escrow-" + suffix + "@test.com").password("pw").nickname("e" + suffix)
                .point(point).build());
    }

    /** 수락 시점처럼 질문자 포인트를 차감하고 SOS_ESCROW 이력을 남긴 게시글을 만든다 */
    private SosPost saveAcceptedPost(User asker, int pointOffer) {
        SosPost post = sosPostRepository.save(SosPost.builder()
                .locationId(1L).locationName("한빛식당").user(asker).content("자리 있나요?")
                .pointOffer(pointOffer).status(SosPostStatus.ACCEPTED)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build());
        asker.deductPoint(pointOffer);
        pointTransactionRepository.save(PointTransaction.builder()
                .user(asker).type(PointTransactionType.SOS_ESCROW).amount(-pointOffer)
                .sosPostId(post.getId()).description("SOS 거래 수락 - 에스크로 보관")
                .build());
        return post;
    }
}
