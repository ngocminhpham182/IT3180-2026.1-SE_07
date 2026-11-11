package com.example.bluemoon.exception;

public class ResidentNotInHouseholdException extends RuntimeException {
    public ResidentNotInHouseholdException(String message) {
        super(message);
    }
}
