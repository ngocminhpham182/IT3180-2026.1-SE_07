package com.example.bluemoon.exception;

public class BillingPeriodClosedException extends RuntimeException {
    public BillingPeriodClosedException() {
        super("Kỳ thu đã đóng, không thể lập danh sách khoản thu");
    }
}
