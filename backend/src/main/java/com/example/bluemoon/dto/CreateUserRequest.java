package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.RoleCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(

        @NotBlank(message = "Username không được để trống")
        @Size(max = 100)
        String username,

        @NotBlank(message = "Password không được để trống")
        @Size(min = 6, max = 128)
        String password,

        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150)
        String fullName,

        @Size(max = 255)
        String email,

        @Size(max = 20)
        String phone,

        Long residentId,

        @NotEmpty(message = "Phải có ít nhất một vai trò")
        Set<RoleCode> roles
) {
}
