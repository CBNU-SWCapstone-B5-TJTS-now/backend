package com.nowhere.backend.repository;

import com.nowhere.backend.domain.entity.Location;
import com.nowhere.backend.domain.entity.LocationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findAllByStatus(LocationStatus status);
}
