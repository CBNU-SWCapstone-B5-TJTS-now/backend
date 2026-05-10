package com.nowhere.backend.controller;

import com.nowhere.backend.dto.request.PeerReviewRequest;
import com.nowhere.backend.dto.response.PeerReviewResponse;
import com.nowhere.backend.service.PeerReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class PeerReviewController {

    private final PeerReviewService peerReviewService;

    @PostMapping
    public ResponseEntity<PeerReviewResponse> createReview(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody PeerReviewRequest request
    ) {
        PeerReviewResponse response = peerReviewService.createReview(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
