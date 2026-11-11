package com.example.bluemoon.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateHouseholdRequest(

        @NotBlank(message = "Mã hộ không được để trống")
        @Size(max = 30, message = "Mã hộ tối đa 30 ký tự")
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "Mã hộ chỉ được chứa chữ cái, chữ số, dấu chấm, gạch dưới và gạch ngang"
        )
        String householdCode,

        @NotBlank(message = "Số căn hộ không được để trống")
        @Size(max = 20, message = "Số căn hộ tối đa 20 ký tự")
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "Số căn hộ chỉ được chứa chữ cái, chữ số, dấu chấm, gạch dưới và gạch ngang"
        )
        String apartmentNo,

        @Min(value = 1, message = "Tầng phải từ 1 đến 60")
        @Max(value = 60, message = "Tầng phải từ 1 đến 60")
        Short floor,

        @NotNull(message = "Diện tích không được để trống")
        @DecimalMin(value = "0", inclusive = false, message = "Diện tích phải lớn hơn 0")
        @DecimalMax(value = "1000", message = "Diện tích tối đa là 1000")
        @Digits(integer = 4, fraction = 2, message = "Diện tích tối đa 2 chữ số thập phân")
        BigDecimal area

) {
}