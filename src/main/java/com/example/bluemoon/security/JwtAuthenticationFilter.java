package com.example.bluemoon.security;

import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Không có token -> endpoint public/protected sẽ do Spring Security xử lý
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // 1. Kiểm tra JWT
            if (!jwtService.isTokenValid(token)) {
                writeUnauthorized(response, "Token không hợp lệ hoặc đã hết hạn");
                return;
            }

            String username = jwtService.extractUsername(token);

            // 2. Luôn đọc user từ DB
            AppUser user = userRepository
                    .findByUsernameIgnoreCase(username)
                    .orElse(null);

            // User bị xóa hoặc bị khóa -> token vô hiệu ngay
            if (user == null || !user.isActive()) {
                writeUnauthorized(
                        response,
                        "Tài khoản không tồn tại hoặc đã bị khóa"
                );
                return;
            }

            // 3. Kiểm tra token được cấp trước khi đổi/reset password
            Instant tokenIssuedAt = jwtService.extractIssuedAt(token);

            if (user.getPasswordChangedAt() != null
                    && tokenIssuedAt.isBefore(user.getPasswordChangedAt())) {

                writeUnauthorized(response, "Token không còn hợp lệ");
                return;
            }

            // 4. Lấy role mới nhất từ DB
            List<SimpleGrantedAuthority> authorities = user.getRoles()
                    .stream()
                    .map(role -> new SimpleGrantedAuthority(
                            "ROLE_" + role.getCode()
                    ))
                    .toList();

            // 5. Set authentication
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(),
                            null,
                            authorities
                    );

            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // Không để exception JWT/JPA biến thành 403 hoặc 500 khó hiểu
            SecurityContextHolder.clearContext();

            writeUnauthorized(response, "Token không hợp lệ");
        }
    }

    private void writeUnauthorized(
            HttpServletResponse response,
            String message
    ) throws IOException {

        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write("""
                {
                  "success": false,
                  "code": "TOKEN_INVALID",
                  "message": "%s"
                }
                """.formatted(message));
    }
}