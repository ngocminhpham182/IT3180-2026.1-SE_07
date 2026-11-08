package com.example.bluemoon.exception;

import java.util.Collections;
import java.util.Map;

public class AppValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public AppValidationException(Map<String, String> errors) {
        super("Dữ liệu không hợp lệ");
        this.errors = errors != null ? errors : Collections.emptyMap();
    }

    public AppValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors != null ? errors : Collections.emptyMap();
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
