package com.nowhere.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nowhere.backend.dto.response.CongestionSseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService implements MessageListener {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30분
    private static final String EVENT_NAME = "congestion-update";

    private final ObjectMapper objectMapper;
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    // Redis Pub/Sub 메시지 수신 → 모든 SSE 클라이언트에 브로드캐스트
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            CongestionSseEvent event = objectMapper.readValue(message.getBody(), CongestionSseEvent.class);
            broadcast(event);
        } catch (IOException e) {
            log.error("SSE 이벤트 역직렬화 실패: {}", e.getMessage());
        }
    }

    private void broadcast(CongestionSseEvent event) {
        CopyOnWriteArrayList<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(EVENT_NAME)
                        .data(event));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });

        emitters.removeAll(deadEmitters);
    }
}
