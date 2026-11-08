package com.example.bluemoon.exception;


public class ResidentLinkRequiredException extends RuntimeException {

    public ResidentLinkRequiredException() {
        super("Tài khoản cư dân phải được liên kết với nhân khẩu");
    }
}