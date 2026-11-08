package com.example.bluemoon.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @NotBlank(message = "Họ tên không được để trống")
        @Size(min = 2, max = 150, message = "Họ tên phải có từ 2 đến 150 ký tự")
        String fullName,

        @Email(message = "Email không đúng định dạng")
        String email,

        @Pattern(
                regexp = "^(0\\d{9}|\\+84\\d{9})$",
                message = "Số điện thoại không đúng định dạng"
        )
        String phone

) {
}