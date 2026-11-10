
package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.CalculationMethod;

import java.math.BigDecimal;

public record FeeCalculationPreviewResponse(
        Long householdId,
        String householdCode,
        String apartmentNo,

        Long feeCategoryId,
        String feeCode,
        String feeName,

        CalculationMethod calculationMethod,

        BigDecimal area,
        BigDecimal unitPrice,
        BigDecimal amount,

        String formula
) {}
