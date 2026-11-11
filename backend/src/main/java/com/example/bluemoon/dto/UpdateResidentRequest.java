package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.Gender;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateResidentRequest(

        @NotBlank(message = "Họ tên không được để trống")
        @Size(min = 2, max = 150, message = "Họ tên phải có từ 2 đến 150 ký tự")
        String fullName,

        @NotNull(message = "Ngày sinh không được để trống")
        @PastOrPresent(message = "Ngày sinh không được ở tương lai")
        LocalDate dateOfBirth,

        Gender gender,

        @Pattern(
                regexp = "^(\\d{9}|\\d{12})$",
                message = "Số CCCD/CMND phải gồm 9 hoặc 12 chữ số"
        )
        String identityNumber,

        @Pattern(
                regexp = "^(0\\d{9}|\\+84\\d{9})$",
                message = "Số điện thoại không hợp lệ"
        )
        String phone,

        @NotBlank(message = "Quan hệ với chủ hộ không được để trống")
        @Size(min = 1, max = 50,
                message = "Quan hệ với chủ hộ tối đa 50 ký tự")
        String relationshipToHead

) {
    @AssertTrue(message = "Ngày sinh phải từ 01/01/1900 đến hiện tại")
    public boolean isDateOfBirthInRange() {
        return dateOfBirth == null ||
                (!dateOfBirth.isBefore(LocalDate.of(1900, 1, 1))
                        && !dateOfBirth.isAfter(LocalDate.now()));
    }
}