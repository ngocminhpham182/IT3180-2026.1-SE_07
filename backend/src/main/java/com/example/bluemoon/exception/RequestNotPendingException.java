package com.example.bluemoon.exception;

import com.example.bluemoon.model.enums.ResidentRequestStatus;
import lombok.Getter;

@Getter
public class RequestNotPendingException extends RuntimeException {
    private final ResidentRequestStatus status;

    public RequestNotPendingException(ResidentRequestStatus status) {
        super("Đề nghị này đã được xử lý");
        this.status = status;
    }
}
