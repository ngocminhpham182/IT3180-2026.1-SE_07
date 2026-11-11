package com.example.bluemoon.exception;

public class FeeCategoryDuplicateException extends RuntimeException {

    public FeeCategoryDuplicateException(String message) {
        super(message);
    }
}