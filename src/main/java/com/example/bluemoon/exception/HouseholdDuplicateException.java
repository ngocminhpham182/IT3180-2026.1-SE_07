package com.example.bluemoon.exception;

import java.util.Map;

public class HouseholdDuplicateException extends RuntimeException {

    private final Map<String, String> errors;

    public HouseholdDuplicateException(Map<String, String> errors) {
        super("Thông tin hộ gia đình hoặc chủ hộ đã tồn tại");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
