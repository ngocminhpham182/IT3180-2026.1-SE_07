package com.example.bluemoon.dto;

import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String phone,
        Long residentId,
        Long householdId,
        boolean active,
        List<String> roles,
        boolean mustChangePassword
) {
}
