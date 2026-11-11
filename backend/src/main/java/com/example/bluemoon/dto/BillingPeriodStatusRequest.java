package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.BillingPeriodStatus;
import jakarta.validation.constraints.NotNull;

public record BillingPeriodStatusRequest(
        @NotNull(message = "Trạng thái kỳ thu không được để trống")
        BillingPeriodStatus status
) {}
