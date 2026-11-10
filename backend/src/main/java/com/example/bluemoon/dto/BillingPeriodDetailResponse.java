package com.example.bluemoon.dto;

import com.example.bluemoon.model.BillingPeriod;
import com.example.bluemoon.model.enums.BillingPeriodStatus;

import java.time.Instant;
import java.time.LocalDate;

public record BillingPeriodDetailResponse(
        Long id,
        String code,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        BillingPeriodStatus status,
        boolean generated,
        Instant generatedAt,
        UserBriefResponse generatedBy,
        Instant closedAt,
        UserBriefResponse closedBy,
        BillingPeriodSummaryResponse summary
) {
    public static BillingPeriodDetailResponse from(
            BillingPeriod period,
            BillingPeriodSummaryResponse summary
    ) {
        return new BillingPeriodDetailResponse(
                period.getId(),
                period.getCode(),
                period.getName(),
                period.getStartDate(),
                period.getEndDate(),
                period.getStatus(),
                period.getGeneratedAt() != null,
                period.getGeneratedAt(),
                period.getGeneratedBy() == null
                        ? null
                        : new UserBriefResponse(
                        period.getGeneratedBy().getId(),
                        period.getGeneratedBy().getFullName()
                ),
                period.getClosedAt(),
                period.getClosedBy() == null
                        ? null
                        : new UserBriefResponse(
                        period.getClosedBy().getId(),
                        period.getClosedBy().getFullName()
                ),
                summary
        );
    }
}