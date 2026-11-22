package com.example.bluemoon.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        boolean success,
        String code,
        String message,
        Map<String, String> errors,
        Object details
) {
    public ApiErrorResponse(boolean success, String code, String message, Object details) {
        this(success, code, message, null, details);
    }

    public ApiErrorResponse(boolean success, String code, String message) {
        this(success, code, message, null, null);
    }
}
