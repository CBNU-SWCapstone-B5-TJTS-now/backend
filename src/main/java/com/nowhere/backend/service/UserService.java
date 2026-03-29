package com.nowhere.backend.service;

import com.nowhere.backend.domain.entity.User;
import com.nowhere.backend.dto.request.SignupRequest;
import com.nowhere.backend.exception.BusinessException;
import com.nowhere.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("이미 사용 중인 이메일입니다", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new BusinessException("이미 사용 중인 닉네임입니다", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .build();

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("존재하지 않는 사용자입니다", HttpStatus.NOT_FOUND));
    }
}
