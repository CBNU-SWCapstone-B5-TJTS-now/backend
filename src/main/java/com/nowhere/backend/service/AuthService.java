package com.nowhere.backend.service;

import com.nowhere.backend.domain.entity.User;
import com.nowhere.backend.dto.request.LoginRequest;
import com.nowhere.backend.dto.request.SignupRequest;
import com.nowhere.backend.dto.response.AuthResponse;
import com.nowhere.backend.exception.BusinessException;
import com.nowhere.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse signup(SignupRequest request) {
        User user = userService.signup(request);
        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getNickname());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userService.findByEmail(request.email());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException("이메일 또는 비밀번호가 올바르지 않습니다", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getNickname());
    }
}
