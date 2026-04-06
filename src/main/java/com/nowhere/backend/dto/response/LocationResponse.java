package com.nowhere.backend.dto.response;

import com.nowhere.backend.domain.entity.CongestionLevel;
import com.nowhere.backend.domain.entity.Location;
import com.nowhere.backend.domain.entity.LocationCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LocationResponse(
        Long id,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        LocationCategory category,
        Integer geofenceRadius,
        Integer defaultTtlMinutes,
        Integer maxTtlMinutes,
        CongestionLevel congestionLevel,
        int approvalCount,
        int rejectionCount,
        LocalDateTime congestionUpdatedAt
) {
    public static LocationResponse of(Location location, CongestionInfo congestion) {
        if (congestion != null) {
            return new LocationResponse(
                    location.getId(),
                    location.getName(),
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getCategory(),
                    location.getGeofenceRadius(),
                    location.getDefaultTtlMinutes(),
                    location.getMaxTtlMinutes(),
                    congestion.getLevel(),
                    congestion.getApprovalCount(),
                    congestion.getRejectionCount(),
                    congestion.getUpdatedAt()
            );
        }
        return new LocationResponse(
                location.getId(),
                location.getName(),
                location.getLatitude(),
                location.getLongitude(),
                location.getCategory(),
                location.getGeofenceRadius(),
                location.getDefaultTtlMinutes(),
                location.getMaxTtlMinutes(),
                null, 0, 0, null
        );
    }
}
