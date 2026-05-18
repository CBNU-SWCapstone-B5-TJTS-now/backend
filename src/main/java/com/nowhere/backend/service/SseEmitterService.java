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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService implements MessageListener {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;
    private static final String EVENT_CONGESTION = "congestion-update";
    private static final String EVENT_NOTIFICATION = "location-notification";

    private final ObjectMapper objectMapper;
    private final SubscriptionRedisService subscriptionRedisService;

    /** 지도용: 인증 없이 전체 혼잡도 업데이트 수신 */
    private final CopyOnWriteArrayList<SseEmitter> publicEmitters = new CopyOnWriteArrayList<>();

    /** 알림용: userId → SseEmitter (구독 장소 알림 수신) */
    private final ConcurrentHashMap<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    /** 기존 공개 SSE 연결 (지도 혼잡도 업데이트용) */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        publicEmitters.add(emitter);
        emitter.onCompletion(() -> publicEmitters.remove(emitter));
        emitter.onTimeout(() -> publicEmitters.remove(emitter));
        emitter.onError(e -> publicEmitters.remove(emitter));
        sendConnectedEvent(emitter);
        return emitter;
    }

    /** EX-2-2: 인증된 사용자용 SSE 연결 (구독 장소 알림 전용) */
    public SseEmitter subscribeAsUser(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        userEmitters.put(userId, emitter);
        emitter.onCompletion(() -> userEmitters.remove(userId));
        emitter.onTimeout(() -> userEmitters.remove(userId));
        emitter.onError(e -> userEmitters.remove(userId));
        sendConnectedEvent(emitter);
        return emitter;
    }

    /** Redis Pub/Sub 메시지 수신 */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            CongestionSseEvent event = objectMapper.readValue(message.getBody(), CongestionSseEvent.class);
            broadcastToPublic(event);
            notifySubscribers(event);
        } catch (IOException e) {
            log.error("SSE 이벤트 역직렬화 실패: {}", e.getMessage());
        }
    }

    /** 지도용: 전체 공개 emitter에 혼잡도 업데이트 브로드캐스트 */
    private void broadcastToPublic(CongestionSseEvent event) {
        CopyOnWriteArrayList<SseEmitter> dead = new CopyOnWriteArrayList<>();
        publicEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(EVENT_CONGESTION).data(event));
            } catch (IOException e) {
                dead.add(emitter);
            }
        });
        publicEmitters.removeAll(dead);
    }

    /** EX-2-2: 해당 장소 구독자에게만 알림 SSE 발송 */
    private void notifySubscribers(CongestionSseEvent event) {
        Set<Long> subscribers = subscriptionRedisService.getSubscribers(event.getLocationId());
        subscribers.forEach(userId -> {
            SseEmitter emitter = userEmitters.get(userId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event().name(EVENT_NOTIFICATION).data(event));
                } catch (IOException e) {
                    userEmitters.remove(userId);
                }
            }
        });
    }

    private void sendConnectedEvent(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            log.warn("SSE 초기 연결 이벤트 전송 실패");
        }
    }
}
