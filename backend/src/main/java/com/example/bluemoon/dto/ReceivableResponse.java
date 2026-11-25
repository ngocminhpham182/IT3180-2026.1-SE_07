package com.example.bluemoon.dto;

import com.example.bluemoon.model.Receivable;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReceivableResponse(
        Long id,
        Long periodId,
        Long householdId,
        String householdCode,
        String apartmentNo,
        Long feeCategoryId,
        String feeCode,
        String feeName,
        BigDecimal quantity,
        BigDecimal unitPriceApplied,
        BigDecimal amountDue,
        LocalDate dueDate,
        BigDecimal paidAmount,
        BigDecimal outstanding
) {
    public static ReceivableResponse from(Receivable r) {
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal due = r.getAmountDue() != null ? r.getAmountDue() : BigDecimal.ZERO;
        BigDecimal outstanding = due.subtract(paidAmount);

        return new ReceivableResponse(
                r.getId(),
                r.getBillingPeriod() != null ? r.getBillingPeriod().getId() : null,
                r.getHousehold() != null ? r.getHousehold().getId() : null,
                r.getHousehold() != null ? r.getHousehold().getHouseholdCode() : null,
                r.getHousehold() != null ? r.getHousehold().getApartmentNo() : null,
                r.getFeeCategory() != null ? r.getFeeCategory().getId() : null,
                r.getFeeCategory() != null ? r.getFeeCategory().getCode() : null,
                r.getFeeCategory() != null ? r.getFeeCategory().getName() : null,
                r.getQuantity(),
                r.getUnitPriceApplied(),
                due,
                r.getDueDate(),
                paidAmount,
                outstanding
        );
    }
}
