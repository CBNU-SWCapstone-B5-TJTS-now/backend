package com.nowhere.backend.service;

import com.nowhere.backend.dto.response.SosReplyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * EX-3-2: SOS 게시글 내 실시간 답변 스트리밍
 * postId → 해당 게시글을 보고 있는 SSE 연결 목록
 */
@Slf4j
@Service
public class SosNotificationService {

    private static final long SSE_TIMEOUT = 10 * 60 * 1000L; // 10분
    private static final String EVENT_NEW_REPLY = "new-reply";

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> postEmitters =
            new ConcurrentHashMap<>();

    public SseEmitter subscribePost(Long postId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        postEmitters.computeIfAbsent(postId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(postId, emitter));
        emitter.onTimeout(() -> removeEmitter(postId, emitter));
        emitter.onError(e -> removeEmitter(postId, emitter));

        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            removeEmitter(postId, emitter);
        }

        return emitter;
    }

    public void notifyNewReply(Long postId, SosReplyResponse reply) {
        CopyOnWriteArrayList<SseEmitter> emitters = postEmitters.get(postId);
        if (emitters == null || emitters.isEmpty()) return;

        CopyOnWriteArrayList<SseEmitter> dead = new CopyOnWriteArrayList<>();
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(EVENT_NEW_REPLY).data(reply));
            } catch (IOException e) {
                dead.add(emitter);
            }
        });
        emitters.removeAll(dead);
    }

    private void removeEmitter(Long postId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = postEmitters.get(postId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) postEmitters.remove(postId);
        }
    }
}
