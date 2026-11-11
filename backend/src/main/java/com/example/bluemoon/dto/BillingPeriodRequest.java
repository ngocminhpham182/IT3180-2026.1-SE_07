package com.example.bluemoon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BillingPeriodRequest(

        @NotBlank(message = "Mã kỳ thu không được để trống")
        @Size(max = 30, message = "Mã kỳ thu không được vượt quá 30 ký tự")
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "Mã kỳ thu chỉ được chứa chữ, số, dấu chấm, gạch dưới và gạch ngang"
        )
        String code,

        @NotBlank(message = "Tên kỳ thu không được để trống")
        @Size(max = 100, message = "Tên kỳ thu không được vượt quá 100 ký tự")
        String name,

        @NotNull(message = "Ngày bắt đầu không được để trống")
        LocalDate startDate,

        @NotNull(message = "Ngày kết thúc không được để trống")
        LocalDate endDate
) {}