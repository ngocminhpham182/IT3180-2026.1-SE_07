package com.example.bluemoon.exception;


public class SelfLockoutException extends RuntimeException {

    public SelfLockoutException() {
        super("Không thể tự bỏ vai trò BQT của chính mình");
    }
}
