package com.example.bluemoon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Username không được để trống")
        @Size(max = 100)
        String username,

        @NotBlank(message = "Password không được để trống")
        @Size(max = 128)
        String password
) {
}