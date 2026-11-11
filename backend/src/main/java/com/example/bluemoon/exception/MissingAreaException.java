package com.example.bluemoon.exception;

public class MissingAreaException extends RuntimeException {

    private final Long householdId;

    public MissingAreaException(Long householdId) {
        super("Hộ gia đình chưa được cập nhật diện tích");
        this.householdId = householdId;
    }

    public Long getHouseholdId() {
        return householdId;
    }
}