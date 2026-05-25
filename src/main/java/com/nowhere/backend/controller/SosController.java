package com.nowhere.backend.controller;

import com.nowhere.backend.dto.request.SosPostRequest;
import com.nowhere.backend.dto.request.SosReplyRequest;
import com.nowhere.backend.dto.response.PointTransactionResponse;
import com.nowhere.backend.dto.response.SosPostResponse;
import com.nowhere.backend.dto.response.SosReplyResponse;
import com.nowhere.backend.service.SosNotificationService;
import com.nowhere.backend.service.SosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/sos")
@RequiredArgsConstructor
public class SosController {

    private final SosService sosService;
    private final SosNotificationService sosNotificationService;

    /** EX-3-1: 장소별 게시글 목록 */
    @GetMapping("/posts")
    public ResponseEntity<List<SosPostResponse>> getPosts(@RequestParam Long locationId) {
        return ResponseEntity.ok(sosService.getPostsByLocation(locationId));
    }

    /** EX-3-1: 게시글 작성 */
    @PostMapping("/posts")
    public ResponseEntity<SosPostResponse> createPost(
            @AuthenticationPrincipal String email,
            @RequestBody SosPostRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sosService.createPost(email, request));
    }

    /** EX-3-1: 게시글 상세 조회 */
    @GetMapping("/posts/{postId}")
    public ResponseEntity<SosPostResponse> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(sosService.getPostDetail(postId));
    }

    /** EX-3-2: 실시간 답변 스트리밍 SSE */
    @GetMapping(value = "/posts/{postId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamReplies(@PathVariable Long postId) {
        return sosNotificationService.subscribePost(postId);
    }

    /** EX-3-1: 답변 작성 */
    @PostMapping("/posts/{postId}/replies")
    public ResponseEntity<SosReplyResponse> createReply(
            @AuthenticationPrincipal String email,
            @PathVariable Long postId,
            @RequestBody SosReplyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sosService.createReply(email, postId, request));
    }

    /** EX-3-4, EX-3-5: 답변 수락 + 에스크로 포인트 차감 */
    @PostMapping("/posts/{postId}/replies/{replyId}/accept")
    public ResponseEntity<SosPostResponse> acceptReply(
            @AuthenticationPrincipal String email,
            @PathVariable Long postId,
            @PathVariable Long replyId
    ) {
        return ResponseEntity.ok(sosService.acceptReply(email, postId, replyId));
    }

    /** EX-3-4: 거래 완료 + 포인트 응답자에게 지급 */
    @PostMapping("/posts/{postId}/complete")
    public ResponseEntity<SosPostResponse> completePost(
            @AuthenticationPrincipal String email,
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(sosService.completePost(email, postId));
    }

    /** EX-3-6: 거래 취소 + 포인트 환불 */
    @PostMapping("/posts/{postId}/cancel")
    public ResponseEntity<SosPostResponse> cancelPost(
            @AuthenticationPrincipal String email,
            @PathVariable Long postId
    ) {
        return ResponseEntity.ok(sosService.cancelPost(email, postId));
    }

    /** EX-3-7: 내 포인트 거래 내역 */
    @GetMapping("/transactions")
    public ResponseEntity<List<PointTransactionResponse>> getMyTransactions(
            @AuthenticationPrincipal String email
    ) {
        return ResponseEntity.ok(sosService.getMyTransactions(email));
    }
}
