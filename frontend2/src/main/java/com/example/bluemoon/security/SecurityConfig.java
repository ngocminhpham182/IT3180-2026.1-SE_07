package com.example.bluemoon.security;

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

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()
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
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }
}