package com.nowhere.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * EX-2-3: 장소별 구독자 목록을 Redis Set으로 캐싱
 * Key: subscription:location:{locationId} → Set<userId>
 */
@Service
@RequiredArgsConstructor
public class SubscriptionRedisService {

    private static final String KEY_PREFIX = "subscription:location:";

    private final RedisTemplate<String, Object> redisTemplate;

    public void addSubscriber(Long locationId, Long userId) {
        redisTemplate.opsForSet().add(key(locationId), userId.toString());
    }

    public void removeSubscriber(Long locationId, Long userId) {
        redisTemplate.opsForSet().remove(key(locationId), userId.toString());
    }

    public Set<Long> getSubscribers(Long locationId) {
        Set<Object> members = redisTemplate.opsForSet().members(key(locationId));
        if (members == null || members.isEmpty()) return Collections.emptySet();
        return members.stream()
                .map(m -> Long.parseLong(m.toString()))
                .collect(Collectors.toSet());
    }

    public boolean isSubscribed(Long locationId, Long userId) {
        Boolean result = redisTemplate.opsForSet().isMember(key(locationId), userId.toString());
        return Boolean.TRUE.equals(result);
    }

    private String key(Long locationId) {
        return KEY_PREFIX + locationId;
    }
}
