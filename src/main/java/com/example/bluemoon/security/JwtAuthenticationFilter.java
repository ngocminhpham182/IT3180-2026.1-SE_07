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

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        String username;
        Instant tokenIssuedAt;

        // 1. Chỉ bắt lỗi liên quan đến JWT
        try {
            if (!jwtService.isTokenValid(token)) {
                writeUnauthorized(response, "Token không hợp lệ hoặc đã hết hạn");
                return;
            }

            username = jwtService.extractUsername(token);
            tokenIssuedAt = jwtService.extractIssuedAt(token);

        } catch (Exception e) {
            writeUnauthorized(response, "Token không hợp lệ");
            return;
        }

        // 2. Truy vấn user từ DB
        // Không đặt trong catch JWT
        AppUser user = userRepository
                .findByUsernameIgnoreCase(username)
                .orElse(null);

        if (user == null || !user.isActive()) {
            writeUnauthorized(
                    response,
                    "Tài khoản không tồn tại hoặc đã bị khóa"
            );
            return;
        }

        // 3. Kiểm tra thời điểm đổi mật khẩu
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
                        user,
                        null,
                        authorities
                );

        System.out.println("JWT USER: " + user.getUsername());

        System.out.println("JWT AUTHORITIES: " + authorities);

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        System.out.println(
                "SECURITY CONTEXT: " +
                        SecurityContextHolder.getContext().getAuthentication()
        );
        
        // 6. Quan trọng: Đặt ngoài try-catch JWT
        filterChain.doFilter(request, response);
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