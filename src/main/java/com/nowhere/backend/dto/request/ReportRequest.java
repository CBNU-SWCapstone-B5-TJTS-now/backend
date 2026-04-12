package com.nowhere.backend.dto.request;

import com.nowhere.backend.domain.entity.CongestionLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportRequest {

    @NotNull(message = "locationId는 필수입니다")
    private Long locationId;

    @NotNull(message = "congestionLevel은 필수입니다")
    private CongestionLevel congestionLevel;

    @NotNull(message = "latitude는 필수입니다")
    private Double latitude;

    @NotNull(message = "longitude는 필수입니다")
    private Double longitude;

    // null이면 장소 기본값 사용
    private Integer customTtlMinutes;
}
