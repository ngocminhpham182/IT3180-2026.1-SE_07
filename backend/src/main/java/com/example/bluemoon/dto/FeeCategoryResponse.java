package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.BillingCycle;
import com.example.bluemoon.model.enums.CalculationMethod;
import com.example.bluemoon.model.enums.FeeKind;

import java.math.BigDecimal;
import java.time.Instant;

public record FeeCategoryResponse(
        Long id,
        String code,
        String name,
        String description,
        FeeKind feeKind,
        CalculationMethod calculationMethod,
        BigDecimal unitPrice,
        BillingCycle billingCycle,
        Integer dueDay,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}