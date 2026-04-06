package com.nowhere.backend.service;

import com.nowhere.backend.domain.entity.Location;
import com.nowhere.backend.domain.entity.LocationStatus;
import com.nowhere.backend.dto.response.CongestionInfo;
import com.nowhere.backend.dto.response.LocationResponse;
import com.nowhere.backend.exception.BusinessException;
import com.nowhere.backend.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final CongestionRedisService congestionRedisService;

    @Transactional(readOnly = true)
    public List<LocationResponse> getAllActiveLocations() {
        return locationRepository.findAllByStatus(LocationStatus.ACTIVE).stream()
                .map(location -> {
                    CongestionInfo congestion = congestionRedisService.find(location.getId()).orElse(null);
                    return LocationResponse.of(location, congestion);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationResponse getLocationById(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("존재하지 않는 장소입니다", HttpStatus.NOT_FOUND));

        CongestionInfo congestion = congestionRedisService.find(id).orElse(null);
        return LocationResponse.of(location, congestion);
    }
}
