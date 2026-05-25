package com.nowhere.backend.service;

import com.nowhere.backend.domain.entity.*;
import com.nowhere.backend.dto.request.SosPostRequest;
import com.nowhere.backend.dto.request.SosReplyRequest;
import com.nowhere.backend.dto.response.PointTransactionResponse;
import com.nowhere.backend.dto.response.SosPostResponse;
import com.nowhere.backend.dto.response.SosReplyResponse;
import com.nowhere.backend.exception.BusinessException;
import com.nowhere.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SosService {

    private static final int TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final SosPostRepository sosPostRepository;
    private final SosReplyRepository sosReplyRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final SosRedisService sosRedisService;
    private final SosNotificationService sosNotificationService;

    // ── 게시글 ─────────────────────────────────────────────────

    @Transactional
    public SosPostResponse createPost(String email, SosPostRequest request) {
        User user = getUser(email);

        SosPost post = SosPost.builder()
                .locationId(request.getLocationId())
                .locationName(request.getLocationName())
                .user(user)
                .content(request.getContent())
                .pointOffer(request.getPointOffer())
                .expiresAt(LocalDateTime.now().plusMinutes(TTL_MINUTES))
                .build();

        SosPost saved = sosPostRepository.save(post);
        sosRedisService.registerPostTtl(saved.getId());
        return SosPostResponse.summary(saved);
    }

    @Transactional(readOnly = true)
    public List<SosPostResponse> getPostsByLocation(Long locationId) {
        return sosPostRepository
                .findByLocationIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        locationId, SosPostStatus.OPEN, LocalDateTime.now())
                .stream()
                .map(SosPostResponse::summary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SosPostResponse getPostDetail(Long postId) {
        return SosPostResponse.from(getActivePost(postId));
    }

    // ── 답변 ───────────────────────────────────────────────────

    @Transactional
    public SosReplyResponse createReply(String email, Long postId, SosReplyRequest request) {
        User user = getUser(email);
        SosPost post = getActivePost(postId);

        if (post.getUser().getId().equals(user.getId())) {
            throw new BusinessException("본인 게시글에는 답변할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if (!post.isOpen()) {
            throw new BusinessException("이미 진행 중이거나 종료된 게시글입니다.", HttpStatus.CONFLICT);
        }

        SosReply reply = SosReply.builder()
                .post(post)
                .user(user)
                .content(request.getContent())
                .build();

        SosReply saved = sosReplyRepository.save(reply);
        sosNotificationService.notifyNewReply(postId, SosReplyResponse.from(saved));
        return SosReplyResponse.from(saved);
    }

    // ── 거래 수락 (EX-3-4, EX-3-5) ────────────────────────────

    @Transactional
    public SosPostResponse acceptReply(String email, Long postId, Long replyId) {
        User requester = getUser(email);
        SosPost post = getActivePost(postId);

        if (!post.getUser().getId().equals(requester.getId())) {
            throw new BusinessException("게시글 작성자만 수락할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        if (!post.isOpen()) {
            throw new BusinessException("이미 진행 중이거나 종료된 게시글입니다.", HttpStatus.CONFLICT);
        }

        SosReply reply = sosReplyRepository.findByIdAndPost(replyId, post)
                .orElseThrow(() -> new BusinessException("답변을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // 포인트 에스크로 처리
        if (post.getPointOffer() != null && post.getPointOffer() > 0) {
            if (requester.getPoint() < post.getPointOffer()) {
                throw new BusinessException("포인트가 부족합니다.", HttpStatus.BAD_REQUEST);
            }
            requester.deductPoint(post.getPointOffer());
            sosRedisService.holdEscrow(postId, post.getPointOffer());

            pointTransactionRepository.save(PointTransaction.builder()
                    .user(requester)
                    .type(PointTransactionType.SOS_ESCROW)
                    .amount(-post.getPointOffer())
                    .sosPostId(postId)
                    .description("SOS 거래 수락 - 에스크로 보관")
                    .build());
        }

        reply.accept();
        post.accept();
        return SosPostResponse.from(post);
    }

    // ── 거래 완료 (EX-3-4) ─────────────────────────────────────

    @Transactional
    public SosPostResponse completePost(String email, Long postId) {
        User requester = getUser(email);
        SosPost post = getPost(postId);

        if (!post.getUser().getId().equals(requester.getId())) {
            throw new BusinessException("게시글 작성자만 완료 처리할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        if (post.getStatus() != SosPostStatus.ACCEPTED) {
            throw new BusinessException("수락된 거래만 완료 처리할 수 있습니다.", HttpStatus.CONFLICT);
        }

        // 수락된 답변자 찾기
        SosReply acceptedReply = post.getReplies().stream()
                .filter(SosReply::isAccepted)
                .findFirst()
                .orElseThrow(() -> new BusinessException("수락된 답변을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        User responder = acceptedReply.getUser();

        // 에스크로 포인트 응답자에게 지급
        if (sosRedisService.hasEscrow(postId)) {
            int amount = sosRedisService.getEscrow(postId);
            responder.addPoint(amount);
            sosRedisService.releaseEscrow(postId);

            pointTransactionRepository.save(PointTransaction.builder()
                    .user(responder)
                    .type(PointTransactionType.SOS_COMPLETE)
                    .amount(amount)
                    .sosPostId(postId)
                    .description("SOS 거래 완료 - 포인트 수령")
                    .build());
        }

        acceptedReply.complete();
        post.complete();
        sosRedisService.expirePost(postId);
        return SosPostResponse.from(post);
    }

    // ── 거래 취소/환불 (EX-3-6) ────────────────────────────────

    @Transactional
    public SosPostResponse cancelPost(String email, Long postId) {
        User requester = getUser(email);
        SosPost post = getPost(postId);

        if (!post.getUser().getId().equals(requester.getId())) {
            throw new BusinessException("게시글 작성자만 취소할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        if (post.getStatus() == SosPostStatus.COMPLETED || post.getStatus() == SosPostStatus.CANCELLED) {
            throw new BusinessException("이미 종료된 거래입니다.", HttpStatus.CONFLICT);
        }

        // 에스크로 포인트 환불
        if (sosRedisService.hasEscrow(postId)) {
            int amount = sosRedisService.getEscrow(postId);
            requester.addPoint(amount);
            sosRedisService.releaseEscrow(postId);

            pointTransactionRepository.save(PointTransaction.builder()
                    .user(requester)
                    .type(PointTransactionType.SOS_REFUND)
                    .amount(amount)
                    .sosPostId(postId)
                    .description("SOS 거래 취소 - 포인트 환불")
                    .build());
        }

        post.cancel();
        sosRedisService.expirePost(postId);
        return SosPostResponse.from(post);
    }

    // ── 거래 내역 (EX-3-7) ─────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PointTransactionResponse> getMyTransactions(String email) {
        User user = getUser(email);
        return pointTransactionRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(PointTransactionResponse::from)
                .collect(Collectors.toList());
    }

    // ── 공통 헬퍼 ──────────────────────────────────────────────

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private SosPost getPost(Long postId) {
        return sosPostRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("게시글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private SosPost getActivePost(Long postId) {
        SosPost post = getPost(postId);
        if (post.isExpired() && post.isOpen()) {
            post.cancel();
        }
        return post;
    }
}
