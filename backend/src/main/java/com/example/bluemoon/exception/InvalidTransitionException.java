package com.example.bluemoon.exception;

import java.util.List;

public class InvalidTransitionException extends RuntimeException {

    private final String currentStatus;
    private final List<String> allowedFrom;

    public InvalidTransitionException(String currentStatus, List<String> allowedFrom) {
        super("Biến động không hợp lệ với trạng thái hiện tại");
        this.currentStatus = currentStatus;
        this.allowedFrom = allowedFrom;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public List<String> getAllowedFrom() {
        return allowedFrom;
    }
}
