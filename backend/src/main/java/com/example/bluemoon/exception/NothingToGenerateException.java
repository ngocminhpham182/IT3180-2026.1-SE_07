package com.example.bluemoon.exception;

import com.example.bluemoon.dto.SkippedReceivable;

import java.util.List;

public class NothingToGenerateException extends RuntimeException {

    private final List<SkippedReceivable> skipped;

    public NothingToGenerateException(List<SkippedReceivable> skipped) {
        super("Không có khoản thu nào có thể được lập cho các hộ hiện tại");
        this.skipped = skipped;
    }

    public List<SkippedReceivable> getSkipped() {
        return skipped;
    }
}
