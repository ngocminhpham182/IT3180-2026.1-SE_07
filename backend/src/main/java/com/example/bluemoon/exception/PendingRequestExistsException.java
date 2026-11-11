package com.example.bluemoon.exception;

import lombok.Getter;

@Getter
public class PendingRequestExistsException extends RuntimeException {
    private final Long requestId;

    public PendingRequestExistsException(Long requestId) {
        super("Đã có đề nghị cùng loại đang chờ duyệt cho nhân khẩu này");
        this.requestId = requestId;
    }

    public PendingRequestExistsException(Long requestId, String message) {
        super(message);
        this.requestId = requestId;
    }
}
