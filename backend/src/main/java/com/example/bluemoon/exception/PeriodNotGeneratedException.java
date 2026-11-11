package com.example.bluemoon.exception;

public class PeriodNotGeneratedException extends RuntimeException {

    public PeriodNotGeneratedException() {
        super("Chưa lập danh sách khoản thu nên không thể đóng kỳ");
    }
}
