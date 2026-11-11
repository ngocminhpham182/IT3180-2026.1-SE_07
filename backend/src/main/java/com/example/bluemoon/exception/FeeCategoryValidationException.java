package com.example.bluemoon.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class FeeCategoryValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public FeeCategoryValidationException(Map<String, String> errors) {
        super("Dữ liệu khoản thu không hợp lệ");
        this.errors = errors;
    }
}