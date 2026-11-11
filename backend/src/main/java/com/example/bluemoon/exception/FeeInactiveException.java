package com.example.bluemoon.exception;

public class FeeInactiveException extends RuntimeException {

    public FeeInactiveException() {
        super("Khoản thu đã ngừng sử dụng");
    }
}