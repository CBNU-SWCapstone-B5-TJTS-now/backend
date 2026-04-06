package com.nowhere.backend.config;

import com.nowhere.backend.domain.entity.Location;
import com.nowhere.backend.domain.entity.LocationCategory;
import com.nowhere.backend.domain.entity.LocationStatus;
import com.nowhere.backend.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final LocationRepository locationRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (locationRepository.count() > 0) {
            log.info("[DataInitializer] Location data already exists. Skipping initialization.");
            return;
        }

        List<Location> locations = List.of(
                Location.builder()
                        .name("한빛식당")
                        .latitude(new BigDecimal("36.6276540"))
                        .longitude(new BigDecimal("127.4589540"))
                        .category(LocationCategory.SCHOOL)
                        .geofenceRadius(100)
                        .defaultTtlMinutes(30)
                        .maxTtlMinutes(60)
                        .status(LocationStatus.ACTIVE)
                        .build(),
                Location.builder()
                        .name("중앙도서관")
                        .latitude(new BigDecimal("36.6281650"))
                        .longitude(new BigDecimal("127.4580790"))
                        .category(LocationCategory.SCHOOL)
                        .geofenceRadius(100)
                        .defaultTtlMinutes(60)
                        .maxTtlMinutes(120)
                        .status(LocationStatus.ACTIVE)
                        .build(),
                Location.builder()
                        .name("라운지")
                        .latitude(new BigDecimal("36.6275070"))
                        .longitude(new BigDecimal("127.4586380"))
                        .category(LocationCategory.CAFE)
                        .geofenceRadius(80)
                        .defaultTtlMinutes(60)
                        .maxTtlMinutes(120)
                        .status(LocationStatus.ACTIVE)
                        .build(),
                Location.builder()
                        .name("로이작업실")
                        .latitude(new BigDecimal("36.6322380"))
                        .longitude(new BigDecimal("127.4575860"))
                        .category(LocationCategory.CAFE)
                        .geofenceRadius(80)
                        .defaultTtlMinutes(60)
                        .maxTtlMinutes(120)
                        .status(LocationStatus.ACTIVE)
                        .build()
        );

        locationRepository.saveAll(locations);
        log.info("[DataInitializer] {} locations initialized successfully.", locations.size());
    }
}
