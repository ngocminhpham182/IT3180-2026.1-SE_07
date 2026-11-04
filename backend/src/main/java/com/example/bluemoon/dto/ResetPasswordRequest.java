package com.example.bluemoon.dto;

import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @Size(
                min = 8,
                max = 128,
                message = "Mật khẩu phải có từ 8 đến 128 ký tự"
        )
        String newPassword

) {
}
