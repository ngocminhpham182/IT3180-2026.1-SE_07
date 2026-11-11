package com.example.bluemoon.service;

import com.example.bluemoon.dto.LoginRequest;
import com.example.bluemoon.dto.LoginResponse;
import com.example.bluemoon.dto.UserLoginResponse;
import com.example.bluemoon.exception.InvalidCredentialsException;
import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.repository.AppUserRepository;
import com.example.bluemoon.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse login(LoginRequest request) {

        AppUser user = userRepository
                .findByUsernameIgnoreCase(request.username())
                .orElseThrow(InvalidCredentialsException::new);

        // Tài khoản bị vô hiệu hóa
        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        // Kiểm tra mật khẩu
        if (!passwordEncoder.matches(
                request.password(),
                user.getPasswordHash())) {

            throw new InvalidCredentialsException();
        }

        // Đăng nhập thành công
        Instant now = Instant.now();
        user.setLastLoginAt(now);

        List<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getCode())
                .toList();

        String token = jwtService.generateToken(user);

        Instant expiresAt = now.plusSeconds(
                jwtService.getExpirationSeconds()
        );

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                expiresAt,
                new UserLoginResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        roles,
                        user.isMustChangePassword()
                )
        );
    }
}