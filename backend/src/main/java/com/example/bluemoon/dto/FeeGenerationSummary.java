package com.example.bluemoon.dto;


import java.math.BigDecimal;

public record FeeGenerationSummary(
        Long feeCategoryId,
        String feeCode,
        String feeName,
        long receivableCount,
        BigDecimal totalAmount
) {}

