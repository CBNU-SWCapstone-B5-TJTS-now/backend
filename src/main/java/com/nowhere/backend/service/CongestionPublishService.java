package com.nowhere.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public void publish(Long locationId, CongestionLevel level, int approvalCount) {
        CongestionSseEvent event = new CongestionSseEvent(
                locationId, level, approvalCount, LocalDateTime.now()
        );
        try {
            String message = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(RedisConfig.CONGESTION_CHANNEL, message);
        } catch (JsonProcessingException e) {
            log.error("SSE 이벤트 직렬화 실패: {}", e.getMessage());
        }
    }
}
