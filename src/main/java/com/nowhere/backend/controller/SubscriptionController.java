package com.nowhere.backend.controller;

import com.nowhere.backend.dto.response.SubscriptionResponse;
import com.nowhere.backend.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * EX-2-1, EX-2-2: 장소 구독 ON/OFF 및 구독 목록 조회 API
 */
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /** 장소 구독 등록 */
    @PostMapping("/{locationId}")
    public ResponseEntity<Void> subscribe(
            @AuthenticationPrincipal String email,
            @PathVariable Long locationId
    ) {
        subscriptionService.subscribe(email, locationId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** 장소 구독 해제 */
    @DeleteMapping("/{locationId}")
    public ResponseEntity<Void> unsubscribe(
            @AuthenticationPrincipal String email,
            @PathVariable Long locationId
    ) {
        subscriptionService.unsubscribe(email, locationId);
        return ResponseEntity.noContent().build();
    }

    /** 내 구독 목록 조회 */
    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getMySubscriptions(
            @AuthenticationPrincipal String email
    ) {
        return ResponseEntity.ok(subscriptionService.getMySubscriptions(email));
    }
}
