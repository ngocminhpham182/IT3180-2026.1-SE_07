package com.example.bluemoon.response;

public record ApiErrorResponse(
        boolean success,
        String code,
        String message,
        Object details
) {
}