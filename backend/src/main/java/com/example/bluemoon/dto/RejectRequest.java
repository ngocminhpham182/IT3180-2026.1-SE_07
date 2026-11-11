package com.example.bluemoon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRequest(
        @NotBlank(message = "Vui lòng nhập lý do từ chối")
        @Size(max = 500, message = "Lý do từ chối tối đa 500 ký tự")
        String reason
) {
    public RejectRequest {
        reason = reason != null ? reason.trim() : null;
    }
}
