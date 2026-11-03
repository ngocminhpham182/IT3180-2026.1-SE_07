package com.example.bluemoon.exception;

public class HouseholdInactiveException extends RuntimeException {

    public HouseholdInactiveException() {
        super("Hộ gia đình không hoạt động");
    }
}