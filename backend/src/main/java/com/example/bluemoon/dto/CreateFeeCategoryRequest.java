package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.BillingCycle;
import com.example.bluemoon.model.enums.CalculationMethod;
import com.example.bluemoon.model.enums.FeeKind;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateFeeCategoryRequest(

        @NotBlank(message = "Mã khoản thu không được để trống")
        @Size(min = 2, max = 30, message = "Mã khoản thu phải có từ 2 đến 30 ký tự")
        @Pattern(
                regexp = "[A-Za-z0-9_]+",
                message = "Mã khoản thu chỉ được chứa chữ cái, chữ số và dấu gạch dưới"
        )
        String code,

        @NotBlank(message = "Tên khoản thu không được để trống")
        @Size(max = 150, message = "Tên khoản thu tối đa 150 ký tự")
        String name,

        @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
        String description,

        @NotNull(message = "Loại khoản thu không được để trống")
        FeeKind feeKind,

        @NotNull(message = "Phương thức tính không được để trống")
        CalculationMethod calculationMethod,

        BigDecimal unitPrice,

        BillingCycle billingCycle,

        Integer dueDay
) {}