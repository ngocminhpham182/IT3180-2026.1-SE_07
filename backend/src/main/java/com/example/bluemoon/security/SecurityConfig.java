package com.example.bluemoon.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        return http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("""
                                    {
                                      "success": false,
                                      "code": "UNAUTHORIZED",
                                      "message": "Chưa đăng nhập hoặc phiên làm việc đã hết hạn"
                                    }
                                    """);
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("""
                                    {
                                      "success": false,
                                      "code": "FORBIDDEN",
                                      "message": "Bạn không có quyền thực hiện thao tác này"
                                    }
                                    """);
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()

                        // Movements (US-03)
                        .requestMatchers(HttpMethod.POST, "/residents/*/movements").hasRole("BQT")
                        .requestMatchers(HttpMethod.GET, "/residents/*/movements").hasAnyRole("BQT", "ACCOUNTANT")

                        // Quick Search (US-04)
                        .requestMatchers(HttpMethod.GET, "/search").hasAnyRole("BQT", "ACCOUNTANT")

                        .requestMatchers("/users", "/users/**").hasRole("BQT")
                        .requestMatchers("/roles", "/roles/**").hasRole("BQT")

                        // GET household: BQT và ACCOUNTANT
                        .requestMatchers(HttpMethod.GET, "/households", "/households/**")
                        .hasAnyRole("BQT", "ACCOUNTANT")

                        // POST household: chỉ BQT
                        .requestMatchers(HttpMethod.POST, "/households")
                        .hasRole("BQT")

                        .requestMatchers(HttpMethod.PUT, "/households/**")
                        .hasRole("BQT")

                        .requestMatchers(HttpMethod.POST, "/households/*/residents")
                        .hasRole("BQT")

                        // Các method khác: chỉ BQT (nếu chưa có quy tắc riêng)
                        .requestMatchers("/households", "/households/**")
                        .hasRole("BQT")

                        .requestMatchers(HttpMethod.GET, "/residents", "/residents/**")
                        .hasAnyRole("BQT", "ACCOUNTANT")

                        .requestMatchers(HttpMethod.PUT, "/residents/**")
                        .hasRole("BQT")

                        .requestMatchers(HttpMethod.DELETE, "/residents/**").hasRole("BQT")

                        .requestMatchers(HttpMethod.POST, "/fee-categories")
                        .hasAnyRole("ACCOUNTANT", "BQT")

                        .requestMatchers(HttpMethod.GET, "/fee-categories", "/fee-categories/**")
                        .hasAnyRole("ACCOUNTANT", "BQT")

                        .requestMatchers(HttpMethod.PUT, "/fee-categories/**")
                        .hasAnyRole("ACCOUNTANT", "BQT")

                        .requestMatchers(HttpMethod.PATCH, "/fee-categories/*/status")
                        .hasAnyRole("ACCOUNTANT", "BQT")

                        .requestMatchers(HttpMethod.POST, "/fee-calculation/preview")
                        .hasAnyRole("ACCOUNTANT", "BQT")

                        .requestMatchers(HttpMethod.POST, "/billing-periods")
                        .hasAnyRole("ACCOUNTANT", "BQT")

                        .requestMatchers(HttpMethod.GET, "/billing-periods", "/billing-periods/**")
                        .hasAnyRole("ACCOUNTANT", "BQT")

                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }
}
