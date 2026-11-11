package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.ResidentRequestType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SubmitResidentRequest(
        @NotNull(message = "Loại đề nghị không được để trống")
        ResidentRequestType type,

        Long residentId,

        @Valid
        ResidentInfo profile,

        @Valid
        ResidentInfo person,

        LocalDate startDate,
        LocalDate endDate,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note
) {
    public SubmitResidentRequest {
        note = note != null ? note.trim() : null;
        if (note != null && note.isEmpty()) {
            note = null;
        }
    }
}
