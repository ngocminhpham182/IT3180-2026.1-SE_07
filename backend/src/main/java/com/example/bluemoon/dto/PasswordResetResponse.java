package com.example.bluemoon.dto;

public record PasswordResetResponse(

        Long userId,
        String username,
        String temporaryPassword,
        boolean mustChangePassword

) {
}
