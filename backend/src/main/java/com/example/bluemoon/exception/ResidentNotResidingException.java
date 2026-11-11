package com.example.bluemoon.exception;

public class ResidentNotResidingException extends RuntimeException {

    private final String residencyStatus;

    public ResidentNotResidingException(String residencyStatus) {
        super("Nhân khẩu không còn cư trú");
        this.residencyStatus = residencyStatus;
    }

    public String getResidencyStatus() {
        return residencyStatus;
    }
}
