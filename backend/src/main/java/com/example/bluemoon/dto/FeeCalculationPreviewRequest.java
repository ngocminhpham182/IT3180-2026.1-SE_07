
package com.example.bluemoon.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FeeCalculationPreviewRequest(

        @NotNull(message = "ID hộ gia đình không được để trống")
        @Positive(message = "ID hộ gia đình phải lớn hơn 0")
        Long householdId,

        @NotNull(message = "ID khoản thu không được để trống")
        @Positive(message = "ID khoản thu phải lớn hơn 0")
        Long feeCategoryId

) {}
