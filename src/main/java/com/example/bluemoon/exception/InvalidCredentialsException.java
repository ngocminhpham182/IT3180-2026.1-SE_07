package com.example.bluemoon.exception;

public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Sai tài khoản hoặc mật khẩu");
    }
}
