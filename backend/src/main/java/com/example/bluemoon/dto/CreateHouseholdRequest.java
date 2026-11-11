package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateHouseholdRequest(

        @NotBlank(message = "Mã hộ không được để trống")
        @Size(max = 30, message = "Mã hộ tối đa 30 ký tự")
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "Mã hộ chỉ được chứa chữ, số, dấu chấm, gạch dưới và gạch ngang"
        )
        String householdCode,

        @NotBlank(message = "Số căn hộ không được để trống")
        @Size(max = 20, message = "Số căn hộ tối đa 20 ký tự")
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "Số căn hộ chỉ được chứa chữ, số, dấu chấm, gạch dưới và gạch ngang"
        )
        String apartmentNo,

        @Min(value = 1, message = "Tầng phải từ 1 đến 60")
        @Max(value = 60, message = "Tầng phải từ 1 đến 60")
        Integer floor,

        @NotNull(message = "Diện tích không được để trống")
        @DecimalMin(value = "0", inclusive = false,
                message = "Diện tích phải lớn hơn 0")
        @DecimalMax(value = "1000",
                message = "Diện tích không được vượt quá 1000")
        @Digits(integer = 8, fraction = 2,
                message = "Diện tích tối đa có 2 chữ số thập phân")
        BigDecimal area,

        @NotNull(message = "Thông tin chủ hộ không được để trống")
        @Valid
        HeadRequest head

) {

    public record HeadRequest(

            @NotBlank(message = "Họ tên không được để trống")
            @Size(min = 2, max = 150,
                    message = "Họ tên phải có từ 2 đến 150 ký tự")
            String fullName,

            @NotNull(message = "Ngày sinh không được để trống")
            @PastOrPresent(message = "Ngày sinh không được lớn hơn hôm nay")
            @org.springframework.format.annotation.DateTimeFormat(
                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE
            )
            LocalDate dateOfBirth,

            Gender gender,

            @Pattern(
                    regexp = "^(\\d{9}|\\d{12})$",
                    message = "CCCD/CMND phải gồm 9 hoặc 12 chữ số"
            )
            String identityNumber,

            @Pattern(
                    regexp = "^(0\\d{9}|\\+84\\d{9})$",
                    message = "Số điện thoại không đúng định dạng"
            )
            String phone
    ) {
    }
}