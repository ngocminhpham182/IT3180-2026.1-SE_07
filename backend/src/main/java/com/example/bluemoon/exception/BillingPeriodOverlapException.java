package com.example.bluemoon.exception;

public class BillingPeriodOverlapException extends RuntimeException {

    private final Long overlappingId;
    private final String overlappingCode;

    public BillingPeriodOverlapException(Long overlappingId, String overlappingCode) {
        super("Khoảng thời gian bị chồng lấn với một kỳ thu khác");
        this.overlappingId = overlappingId;
        this.overlappingCode = overlappingCode;
    }

    public Long getOverlappingId() {
        return overlappingId;
    }

    public String getOverlappingCode() {
        return overlappingCode;
    }
}