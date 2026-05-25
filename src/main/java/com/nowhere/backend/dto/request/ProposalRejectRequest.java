package com.nowhere.backend.dto.request;

import jakarta.validation.constraints.Size;

public record ProposalRejectRequest(

        @Size(max = 200, message = "반려 사유는 200자 이하여야 합니다")
        String reason
) {}
