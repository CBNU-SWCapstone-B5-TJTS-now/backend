package com.nowhere.backend.repository;

import com.nowhere.backend.domain.entity.Location;
import com.nowhere.backend.domain.entity.LocationSubscription;
import com.nowhere.backend.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocationSubscriptionRepository extends JpaRepository<LocationSubscription, Long> {

    Optional<LocationSubscription> findByUserAndLocation(User user, Location location);

    List<LocationSubscription> findAllByUser(User user);

    boolean existsByUserAndLocation(User user, Location location);

    void deleteByUserAndLocation(User user, Location location);
}
