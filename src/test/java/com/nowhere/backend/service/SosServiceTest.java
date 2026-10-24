package com.nowhere.backend.service;

import com.nowhere.backend.domain.entity.*;
import com.nowhere.backend.dto.request.SosPostRequest;
import com.nowhere.backend.dto.request.SosReplyRequest;
import com.nowhere.backend.exception.BusinessException;
import com.nowhere.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SOS 현장(지오펜싱) 제한과 에스크로 60분 자동 취소·환불 규칙을 확인한다.
 */
@ExtendWith(MockitoExtension.class)
class SosServiceTest {

    // 한빛식당 중심 좌표, 반경 100m
    private static final double CENTER_LAT = 36.6276540;
    private static final double CENTER_LON = 127.4589540;
    private static final double FAR_LAT = CENTER_LAT + 0.002; // 약 220m 북쪽

    @Mock private UserRepository userRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private SosPostRepository sosPostRepository;
    @Mock private SosReplyRepository sosReplyRepository;
    @Mock private PointTransactionRepository pointTransactionRepository;
    @Mock private SosRedisService sosRedisService;
    @Mock private SosNotificationService sosNotificationService;

    @InjectMocks
    private SosService sosService;

    private User asker;
    private User responder;
    private Location location;

    @BeforeEach
    void setUp() {
        asker = User.builder().id(1L).email("asker@test.com").password("pw").nickname("asker").point(0).build();
        responder = User.builder().id(2L).email("helper@test.com").password("pw").nickname("helper").build();
        location = Location.builder()
                .id(10L).name("한빛식당")
                .latitude(new BigDecimal("36.6276540")).longitude(new BigDecimal("127.4589540"))
                .category(LocationCategory.RESTAURANT)
                .geofenceRadius(100).defaultTtlMinutes(30).maxTtlMinutes(60)
                .build();
    }

    // ── 지오펜싱 제한 ───────────────────────────────────────────

    @Test
    void 반경_밖에서_질문하면_403으로_거부한다() {
        when(userRepository.findByEmail(asker.getEmail())).thenReturn(Optional.of(asker));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> sosService.createPost(asker.getEmail(), postRequest(FAR_LAT, CENTER_LON)))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        verify(sosPostRepository, never()).save(any());
    }

    @Test
    void 반경_안에서_질문하면_저장하고_장소명은_DB_기준으로_기록한다() {
        when(userRepository.findByEmail(asker.getEmail())).thenReturn(Optional.of(asker));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(location));
        when(sosPostRepository.save(any(SosPost.class))).thenAnswer(inv -> inv.getArgument(0));

        sosService.createPost(asker.getEmail(), postRequest(CENTER_LAT, CENTER_LON));

        ArgumentCaptor<SosPost> saved = ArgumentCaptor.forClass(SosPost.class);
        verify(sosPostRepository).save(saved.capture());
        assertThat(saved.getValue().getLocationName()).isEqualTo("한빛식당");
    }

    @Test
    void 반경_밖에서_답변하면_403으로_거부한다() {
        SosPost post = post(SosPostStatus.OPEN, null);
        when(userRepository.findByEmail(responder.getEmail())).thenReturn(Optional.of(responder));
        when(sosPostRepository.findById(100L)).thenReturn(Optional.of(post));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> sosService.createReply(responder.getEmail(), 100L, replyRequest(FAR_LAT, CENTER_LON)))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        verify(sosReplyRepository, never()).save(any());
    }

    // ── 에스크로 60분 자동 취소·환불 ───────────────────────────────

    @Test
    void 수락_후_60분이_지난_거래는_자동_취소하고_제안_포인트를_환불한다() {
        SosPost post = post(SosPostStatus.ACCEPTED, 100);
        when(sosPostRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(post));
        when(pointTransactionRepository.existsBySosPostIdAndTypeAndCreatedAtBefore(
                eq(100L), eq(PointTransactionType.SOS_ESCROW), any(LocalDateTime.class))).thenReturn(true);

        boolean cancelled = sosService.autoCancelTimedOutEscrow(100L);

        assertThat(cancelled).isTrue();
        assertThat(post.getStatus()).isEqualTo(SosPostStatus.CANCELLED);
        assertThat(asker.getPoint()).isEqualTo(100);
        ArgumentCaptor<PointTransaction> tx = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(tx.capture());
        assertThat(tx.getValue().getType()).isEqualTo(PointTransactionType.SOS_REFUND);
        assertThat(tx.getValue().getAmount()).isEqualTo(100);
        verify(sosRedisService).releaseEscrow(100L);
    }

    @Test
    void 이미_완료된_거래는_자동_취소하지_않는다() {
        SosPost post = post(SosPostStatus.COMPLETED, 100);
        when(sosPostRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(post));

        assertThat(sosService.autoCancelTimedOutEscrow(100L)).isFalse();
        assertThat(post.getStatus()).isEqualTo(SosPostStatus.COMPLETED);
        assertThat(asker.getPoint()).isZero();
        verify(pointTransactionRepository, never()).save(any());
    }

    @Test
    void 수락_후_60분이_지난_거래는_완료할_수_없다() {
        SosPost post = post(SosPostStatus.ACCEPTED, 100);
        when(userRepository.findByEmail(asker.getEmail())).thenReturn(Optional.of(asker));
        when(sosPostRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(post));
        when(pointTransactionRepository.existsBySosPostIdAndTypeAndCreatedAtBefore(
                eq(100L), eq(PointTransactionType.SOS_ESCROW), any(LocalDateTime.class))).thenReturn(true);

        assertThatThrownBy(() -> sosService.completePost(asker.getEmail(), 100L))
                .isInstanceOf(BusinessException.class)
                .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        assertThat(responder.getPoint()).isZero();
    }

    @Test
    void 수락된_거래를_취소하면_Redis_키가_없어도_제안_포인트를_환불한다() {
        SosPost post = post(SosPostStatus.ACCEPTED, 100);
        when(userRepository.findByEmail(asker.getEmail())).thenReturn(Optional.of(asker));
        when(sosPostRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(post));

        sosService.cancelPost(asker.getEmail(), 100L);

        assertThat(post.getStatus()).isEqualTo(SosPostStatus.CANCELLED);
        assertThat(asker.getPoint()).isEqualTo(100);
        verify(sosRedisService, never()).hasEscrow(any());
    }

    // ── 헬퍼 ───────────────────────────────────────────────────

    private SosPost post(SosPostStatus status, Integer pointOffer) {
        return SosPost.builder()
                .id(100L).locationId(10L).locationName("한빛식당")
                .user(asker).content("자리 있나요?").pointOffer(pointOffer)
                .status(status).expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
    }

    private SosPostRequest postRequest(double lat, double lon) {
        SosPostRequest request = new SosPostRequest();
        ReflectionTestUtils.setField(request, "locationId", 10L);
        ReflectionTestUtils.setField(request, "content", "자리 있나요?");
        ReflectionTestUtils.setField(request, "latitude", lat);
        ReflectionTestUtils.setField(request, "longitude", lon);
        return request;
    }

    private SosReplyRequest replyRequest(double lat, double lon) {
        SosReplyRequest request = new SosReplyRequest();
        ReflectionTestUtils.setField(request, "content", "2층에 자리 있어요");
        ReflectionTestUtils.setField(request, "latitude", lat);
        ReflectionTestUtils.setField(request, "longitude", lon);
        return request;
    }
}
