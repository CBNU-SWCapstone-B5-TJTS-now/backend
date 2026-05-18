package com.nowhere.backend.service;

import com.nowhere.backend.domain.entity.Location;
import com.nowhere.backend.domain.entity.LocationSubscription;
import com.nowhere.backend.domain.entity.User;
import com.nowhere.backend.dto.response.SubscriptionResponse;
import com.nowhere.backend.exception.BusinessException;
import com.nowhere.backend.repository.LocationRepository;
import com.nowhere.backend.repository.LocationSubscriptionRepository;
import com.nowhere.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final LocationSubscriptionRepository subscriptionRepository;
    private final SubscriptionRedisService subscriptionRedisService;

    @Transactional
    public void subscribe(String email, Long locationId) {
        User user = getUser(email);
        Location location = getLocation(locationId);

        if (subscriptionRepository.existsByUserAndLocation(user, location)) {
            throw new BusinessException("이미 구독 중인 장소입니다.", HttpStatus.CONFLICT);
        }

        subscriptionRepository.save(LocationSubscription.builder()
                .user(user)
                .location(location)
                .build());

        subscriptionRedisService.addSubscriber(locationId, user.getId());
    }

    @Transactional
    public void unsubscribe(String email, Long locationId) {
        User user = getUser(email);
        Location location = getLocation(locationId);

        LocationSubscription subscription = subscriptionRepository
                .findByUserAndLocation(user, location)
                .orElseThrow(() -> new BusinessException("구독 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        subscriptionRepository.delete(subscription);
        subscriptionRedisService.removeSubscriber(locationId, user.getId());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getMySubscriptions(String email) {
        User user = getUser(email);
        return subscriptionRepository.findAllByUser(user).stream()
                .map(SubscriptionResponse::from)
                .collect(Collectors.toList());
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private Location getLocation(Long locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new BusinessException("장소를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }
}
