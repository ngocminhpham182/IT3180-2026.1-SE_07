package com.example.bluemoon.exception;

import java.util.Map;

public class HasTransactionsException extends RuntimeException {
    private final Map<String, String> errors;

    public HasTransactionsException(Map<String, String> errors) {
        super("Không thể thay đổi thông tin định danh của khoản thu đã phát sinh công nợ");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
