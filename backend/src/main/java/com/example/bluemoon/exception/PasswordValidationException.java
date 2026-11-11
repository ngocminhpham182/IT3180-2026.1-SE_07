package com.example.bluemoon.exception;

public class PasswordValidationException extends RuntimeException {

    private final String field;

    public PasswordValidationException(
            String field,
            String message
    ) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}