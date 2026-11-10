package com.example.bluemoon.exception;

public class NoActiveFeeException extends RuntimeException {
    public NoActiveFeeException() {
        super("Chưa có khoản thu bắt buộc nào đang hiệu lực. Hãy tạo khoản thu trước.");
    }
}
