package com.example.bluemoon.dto;


import java.math.BigDecimal;
import java.util.List;

public record BillingGenerationResponse(
        Long billingPeriodId,
        boolean dryRun,
        long generatedReceivables,
        long householdCount,
        BigDecimal totalAmount,
        List<FeeGenerationSummary> byFee,
        List<SkippedReceivable> skipped,
        long inactiveHouseholdCount
) {}

