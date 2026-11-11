package com.example.bluemoon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank(message = "Mật khẩu cũ không được để trống")
        String oldPassword,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(
                min = 8,
                max = 128,
                message = "Mật khẩu mới phải có từ 8 đến 128 ký tự"
        )
        String newPassword,

        @NotBlank(message = "Mật khẩu xác nhận không được để trống")
        String confirmPassword

) {
}