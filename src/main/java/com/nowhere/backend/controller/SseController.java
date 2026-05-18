package com.nowhere.backend.controller;

import com.nowhere.backend.repository.UserRepository;
import com.nowhere.backend.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;
    private final UserRepository userRepository;

    /** 기존: 지도용 공개 SSE (인증 불필요) */
    @GetMapping(value = "/api/locations/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseEmitterService.subscribe();
    }

    /**
     * EX-2-2: 구독 알림 전용 SSE (인증 필요)
     * 연결 후 자신이 구독한 장소의 혼잡도 변경 알림만 수신
     */
    @GetMapping(value = "/api/sse/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter notifications(@AuthenticationPrincipal String email) {
        Long userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."))
                .getId();
        return sseEmitterService.subscribeAsUser(userId);
    }
}
