package com.nowhere.backend.dto.response;

public record AuthResponse(
        String accessToken,
        String nickname,
        String role
) {}
