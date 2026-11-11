package com.example.bluemoon.exception;

public class NoChangesException extends RuntimeException {
    public NoChangesException() {
        super("Chưa có thay đổi nào so với thông tin hiện tại");
    }

    public NoChangesException(String message) {
        super(message);
    }
}
