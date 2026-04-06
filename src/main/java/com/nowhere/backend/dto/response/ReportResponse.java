package com.nowhere.backend.dto.response;

import com.nowhere.backend.domain.entity.CongestionLevel;
import com.nowhere.backend.domain.entity.CongestionReport;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReportResponse {

    private Long reportId;
    private Long locationId;
    private CongestionLevel congestionLevel;
    private LocalDateTime expiresAt;

    public static ReportResponse from(CongestionReport report) {
        return ReportResponse.builder()
                .reportId(report.getId())
                .locationId(report.getLocation().getId())
                .congestionLevel(report.getCongestionLevel())
                .expiresAt(report.getExpiresAt())
                .build();
    }
}
