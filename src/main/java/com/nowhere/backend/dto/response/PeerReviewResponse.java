package com.nowhere.backend.dto.response;

import com.nowhere.backend.domain.entity.PeerReview;
import com.nowhere.backend.domain.entity.ReviewDecision;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PeerReviewResponse {

    private Long reviewId;
    private Long reportId;
    private ReviewDecision decision;
    private int approvalCount;

    public static PeerReviewResponse of(PeerReview review, int approvalCount) {
        return PeerReviewResponse.builder()
                .reviewId(review.getId())
                .reportId(review.getReport().getId())
                .decision(review.getDecision())
                .approvalCount(approvalCount)
                .build();
    }
}
