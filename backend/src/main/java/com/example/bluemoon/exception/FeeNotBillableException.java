package com.example.bluemoon.exception;

public class FeeNotBillableException extends RuntimeException {

    public FeeNotBillableException() {
        super("Khoản thu tự nguyện không được tính công nợ tự động");
    }
}