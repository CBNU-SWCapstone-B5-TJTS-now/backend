package com.nowhere.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * EX-3-3: SOS 게시글 TTL 관리 (30분 자동 삭제)
 * EX-3-5: 포인트 에스크로 임시 보관
 */
@Service
@RequiredArgsConstructor
public class SosRedisService {

    private static final String POST_TTL_KEY = "sos:post:";
    private static final String ESCROW_KEY   = "sos:escrow:";
    private static final long   POST_TTL_MIN = 30L;
    private static final long   ESCROW_TTL_MIN = 60L; // 수락 후 1시간 내 완료 없으면 자동 환불

    private final RedisTemplate<String, Object> redisTemplate;

    // ── 게시글 TTL ────────────────────────────────────────────

    public void registerPostTtl(Long postId) {
        redisTemplate.opsForValue().set(POST_TTL_KEY + postId, "active", POST_TTL_MIN, TimeUnit.MINUTES);
    }

    public boolean isPostActive(Long postId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(POST_TTL_KEY + postId));
    }

    public void expirePost(Long postId) {
        redisTemplate.delete(POST_TTL_KEY + postId);
    }

    // ── 포인트 에스크로 ────────────────────────────────────────

    /** 수락 시 요청자 포인트를 에스크로에 임시 보관 */
    public void holdEscrow(Long postId, int amount) {
        redisTemplate.opsForValue().set(ESCROW_KEY + postId, String.valueOf(amount), ESCROW_TTL_MIN, TimeUnit.MINUTES);
    }

    /** 에스크로에 보관된 포인트 금액 조회 */
    public int getEscrow(Long postId) {
        Object val = redisTemplate.opsForValue().get(ESCROW_KEY + postId);
        if (val == null) return 0;
        return Integer.parseInt(val.toString());
    }

    /** 완료 또는 환불 처리 후 에스크로 삭제 */
    public void releaseEscrow(Long postId) {
        redisTemplate.delete(ESCROW_KEY + postId);
    }

    public boolean hasEscrow(Long postId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ESCROW_KEY + postId));
    }
}
