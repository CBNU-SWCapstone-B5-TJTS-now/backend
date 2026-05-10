package com.nowhere.backend.service;

import com.nowhere.backend.domain.entity.CongestionReport;
import com.nowhere.backend.domain.entity.PeerReview;
import com.nowhere.backend.domain.entity.ReviewDecision;
import com.nowhere.backend.domain.entity.User;
import com.nowhere.backend.dto.request.PeerReviewRequest;
import com.nowhere.backend.dto.response.PeerReviewResponse;
import com.nowhere.backend.exception.BusinessException;
import com.nowhere.backend.repository.CongestionReportRepository;
import com.nowhere.backend.repository.PeerReviewRepository;
import com.nowhere.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PeerReviewService {

    private final PeerReviewRepository peerReviewRepository;
    private final CongestionReportRepository reportRepository;
    private final UserRepository userRepository;
    private final CongestionRedisService congestionRedisService;

    @Transactional
    public PeerReviewResponse createReview(String email, PeerReviewRequest request) {
        User reviewer = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        CongestionReport report = reportRepository.findById(request.getReportId())
                .orElseThrow(() -> new BusinessException("존재하지 않는 제보입니다", HttpStatus.NOT_FOUND));

        if (report.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("만료된 제보입니다", HttpStatus.GONE);
        }

        if (peerReviewRepository.existsByReportIdAndReviewerId(report.getId(), reviewer.getId())) {
            throw new BusinessException("이미 해당 제보에 리뷰를 등록하셨습니다", HttpStatus.CONFLICT);
        }

        PeerReview review = PeerReview.builder()
                .report(report)
                .reviewer(reviewer)
                .decision(request.getDecision())
                .build();
        peerReviewRepository.save(review);

        if (request.getDecision() == ReviewDecision.APPROVE) {
            report.incrementApprovalCount();
        } else {
            report.incrementRejectionCount();
        }

        syncRedisApprovalCount(report);

        return PeerReviewResponse.of(review, report.getApprovalCount());
    }

    private void syncRedisApprovalCount(CongestionReport report) {
        congestionRedisService.find(report.getLocation().getId()).ifPresent(info -> {
            info.setApprovalCount(report.getApprovalCount());
            info.setRejectionCount(report.getRejectionCount());
            long remainingMinutes = java.time.Duration.between(
                    LocalDateTime.now(), report.getExpiresAt()
            ).toMinutes();
            if (remainingMinutes > 0) {
                congestionRedisService.save(report.getLocation().getId(), info, remainingMinutes);
            }
        });
    }
}
