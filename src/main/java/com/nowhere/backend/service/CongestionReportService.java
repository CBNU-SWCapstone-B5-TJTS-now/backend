package com.nowhere.backend.service;

import com.nowhere.backend.domain.entity.CongestionReport;
import com.nowhere.backend.domain.entity.Location;
import com.nowhere.backend.domain.entity.User;
import com.nowhere.backend.dto.request.ReportRequest;
import com.nowhere.backend.dto.response.CongestionInfo;
import com.nowhere.backend.dto.response.ReportResponse;
import com.nowhere.backend.exception.BusinessException;
import com.nowhere.backend.repository.CongestionReportRepository;
import com.nowhere.backend.repository.LocationRepository;
import com.nowhere.backend.repository.UserRepository;
import com.nowhere.backend.util.GeofenceUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CongestionReportService {

    private final CongestionReportRepository reportRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final CongestionRedisService congestionRedisService;
    private final CongestionPublishService congestionPublishService;

    @Transactional
    public ReportResponse createReport(String email, ReportRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new BusinessException("장소를 찾을 수 없습니다", HttpStatus.NOT_FOUND));

        // 지오펜스 검증 → 반경 밖이면 403
        boolean inside = GeofenceUtil.isInsideGeofence(
                request.getLatitude(), request.getLongitude(),
                location.getLatitude().doubleValue(), location.getLongitude().doubleValue(),
                location.getGeofenceRadius()
        );
        if (!inside) {
            throw new BusinessException("해당 장소의 반경 내에서만 제보할 수 있습니다", HttpStatus.FORBIDDEN);
        }

        int ttlMinutes = resolveTtl(request.getCustomTtlMinutes(), location);

        CongestionReport report = CongestionReport.builder()
                .location(location)
                .user(user)
                .congestionLevel(request.getCongestionLevel())
                .customTtlMinutes(request.getCustomTtlMinutes())
                .expiresAt(LocalDateTime.now().plusMinutes(ttlMinutes))
                .build();

        CongestionReport saved = reportRepository.save(report);

        // Redis 업데이트 → 지도 즉시 반영
        CongestionInfo congestionInfo = new CongestionInfo(
                request.getCongestionLevel(),
                0,
                0,
                LocalDateTime.now()
        );
        congestionRedisService.save(location.getId(), congestionInfo, ttlMinutes);
        congestionPublishService.publish(location.getId(), request.getCongestionLevel(), 0);

        return ReportResponse.from(saved);
    }

    private int resolveTtl(Integer customTtlMinutes, Location location) {
        if (customTtlMinutes == null) {
            return location.getDefaultTtlMinutes();
        }
        if (customTtlMinutes > location.getMaxTtlMinutes()) {
            throw new BusinessException(
                    "customTtlMinutes(" + customTtlMinutes + ")가 최대값(" + location.getMaxTtlMinutes() + ")을 초과합니다",
                    HttpStatus.BAD_REQUEST
            );
        }
        return customTtlMinutes;
    }
}
