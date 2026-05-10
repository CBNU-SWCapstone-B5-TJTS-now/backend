package com.nowhere.backend.dto.request;

import com.nowhere.backend.domain.entity.ReviewDecision;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PeerReviewRequest {

    @NotNull(message = "reportId는 필수입니다")
    private Long reportId;

    @NotNull(message = "decision은 필수입니다")
    private ReviewDecision decision;
}
