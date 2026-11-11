package com.example.bluemoon.dto;

import java.math.BigDecimal;

public record BillingPeriodSummaryResponse(
        long receivableCount,
        long householdCount,
        BigDecimal totalDue
) {
    public static BillingPeriodSummaryResponse empty() {
        return new BillingPeriodSummaryResponse(
                0,
                0,
                BigDecimal.ZERO
        );
    }
}