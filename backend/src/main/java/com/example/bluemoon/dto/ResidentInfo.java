package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ResidentInfo(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(min = 2, max = 150, message = "Họ tên phải có từ 2 đến 150 ký tự")
        String fullName,

        @NotNull(message = "Ngày sinh không được để trống")
        LocalDate dateOfBirth,

        Gender gender,

        @Pattern(regexp = "^(\\d{9}|\\d{12})$", message = "Số CCCD/CMND phải gồm 9 hoặc 12 chữ số")
        String identityNumber,

        @Pattern(regexp = "^(0\\d{9}|\\+84\\d{9})$", message = "Số điện thoại không hợp lệ")
        String phone,

        @NotBlank(message = "Quan hệ với chủ hộ không được để trống")
        @Size(min = 1, max = 50, message = "Quan hệ với chủ hộ không được để trống")
        String relationshipToHead
) {
    public ResidentInfo {
        fullName = fullName != null ? fullName.trim() : null;
        relationshipToHead = relationshipToHead != null ? relationshipToHead.trim() : null;
        if (identityNumber != null) {
            identityNumber = identityNumber.trim();
            if (identityNumber.isEmpty()) {
                identityNumber = null;
            }
        }
        if (phone != null) {
            phone = phone.trim();
            if (phone.isEmpty()) {
                phone = null;
            }
        }
    }
}
