package com.example.bluemoon.exception;

public class InvalidBillingPeriodDateRangeException extends RuntimeException {

    public InvalidBillingPeriodDateRangeException() {
        super("Ngày kết thúc không được sớm hơn ngày bắt đầu");
    }
}