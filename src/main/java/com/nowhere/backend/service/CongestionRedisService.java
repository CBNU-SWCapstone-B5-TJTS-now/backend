package com.nowhere.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nowhere.backend.dto.response.CongestionInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CongestionRedisService {

    private static final String KEY_PREFIX = "congestion:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void save(Long locationId, CongestionInfo info, long ttlMinutes) {
        String key = KEY_PREFIX + locationId;
        redisTemplate.opsForValue().set(key, info, ttlMinutes, TimeUnit.MINUTES);
    }

    public Optional<CongestionInfo> find(Long locationId) {
        String key = KEY_PREFIX + locationId;
        Object raw = redisTemplate.opsForValue().get(key);
        if (raw == null) return Optional.empty();
        return Optional.of(objectMapper.convertValue(raw, CongestionInfo.class));
    }

    public void delete(Long locationId) {
        redisTemplate.delete(KEY_PREFIX + locationId);
    }
}
