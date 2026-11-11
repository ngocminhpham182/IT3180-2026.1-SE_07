package com.example.bluemoon.exception;

import java.time.Instant;

public class BillingPeriodAlreadyGeneratedException extends RuntimeException {

    private final Instant generatedAt;

    public BillingPeriodAlreadyGeneratedException(Instant generatedAt) {
        super("Kỳ thu đã được lập danh sách khoản thu");
        this.generatedAt = generatedAt;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
