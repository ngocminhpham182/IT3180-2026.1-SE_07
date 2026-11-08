package com.example.bluemoon.exception;

public class SelfResetNotAllowedException extends RuntimeException {

    public SelfResetNotAllowedException() {
        super("Không thể đặt lại mật khẩu cho chính mình");
    }
}
