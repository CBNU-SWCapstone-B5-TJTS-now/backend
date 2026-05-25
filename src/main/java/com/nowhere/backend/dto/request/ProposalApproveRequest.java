package com.nowhere.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProposalApproveRequest(

        @NotNull(message = "지오펜싱 반경을 입력해주세요")
        @Min(value = 10, message = "지오펜싱 반경은 최소 10m 이상이어야 합니다")
        Integer geofenceRadius,

        @NotNull(message = "기본 TTL을 입력해주세요")
        @Min(value = 1, message = "기본 TTL은 최소 1분 이상이어야 합니다")
        Integer defaultTtlMinutes,

        @NotNull(message = "최대 TTL을 입력해주세요")
        @Min(value = 1, message = "최대 TTL은 최소 1분 이상이어야 합니다")
        Integer maxTtlMinutes
) {}
