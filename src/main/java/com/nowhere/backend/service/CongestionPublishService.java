package com.nowhere.backend.service;

import com.nowhere.backend.config.RedisConfig;
import com.nowhere.backend.domain.entity.CongestionLevel;
import com.nowhere.backend.dto.response.CongestionSseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CongestionPublishService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(Long locationId, CongestionLevel level, int approvalCount) {
        CongestionSseEvent event = new CongestionSseEvent(
                locationId, level, approvalCount, LocalDateTime.now()
        );
        redisTemplate.convertAndSend(RedisConfig.CONGESTION_CHANNEL, event);
    }
}
