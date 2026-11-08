package com.example.bluemoon.exception;

public class InvalidNewHeadException extends RuntimeException {

    private final String reason;

    public InvalidNewHeadException(String message, String reason) {
        super(message);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}