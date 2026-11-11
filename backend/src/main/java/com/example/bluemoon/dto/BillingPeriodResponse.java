package com.example.bluemoon.dto;

import com.example.bluemoon.model.BillingPeriod;
import com.example.bluemoon.model.enums.BillingPeriodStatus;

import java.time.Instant;
import java.time.LocalDate;

public record BillingPeriodResponse(
        Long id,
        String code,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        BillingPeriodStatus status,
        Instant generatedAt,
        Instant closedAt
) {
    public static BillingPeriodResponse from(BillingPeriod period) {
        return new BillingPeriodResponse(
                period.getId(),
                period.getCode(),
                period.getName(),
                period.getStartDate(),
                period.getEndDate(),
                period.getStatus(),
                period.getGeneratedAt(),
                period.getClosedAt()
        );
    }
}