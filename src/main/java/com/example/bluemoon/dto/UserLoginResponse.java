package com.example.bluemoon.dto;

import java.util.List;

public record UserLoginResponse(
        Long id,
        String username,
        String fullName,
        List<String> roles,
        boolean mustChangePassword
) {
}