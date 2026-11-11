package com.example.bluemoon.dto;

import jakarta.validation.constraints.Size;

public record ApproveRequest(
        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note
) {
    public ApproveRequest {
        note = note != null ? note.trim() : null;
        if (note != null && note.isEmpty()) {
            note = null;
        }
    }
}
