package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.MovementType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateResidentMovementRequest(

        @NotNull(message = "Loại biến động không được để trống")
        MovementType movementType,

        LocalDate effectiveDate,

        LocalDate startDate,

        LocalDate endDate,

        Long newHouseholdId,

        Long newHeadResidentId,

        @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
        String note

) {
    @AssertTrue(message = "US-03 không hỗ trợ loại biến động BIRTH")
    public boolean isMovementTypeAllowed() {
        return movementType == null || movementType != MovementType.BIRTH;
    }

    @AssertTrue(message = "Ngày biến động không được để trống")
    public boolean isEffectiveDateRequired() {
        if (movementType == null) return true;

        if (movementType == MovementType.TEMPORARY_ABSENCE
                || movementType == MovementType.TEMPORARY_RESIDENCE) {
            return startDate != null;
        }

        return effectiveDate != null;
    }

    @AssertTrue(message = "Ngày biến động không được ở tương lai")
    public boolean isEffectiveDateNotFuture() {
        LocalDate date = movementType == MovementType.TEMPORARY_ABSENCE
                || movementType == MovementType.TEMPORARY_RESIDENCE
                ? startDate
                : effectiveDate;

        return date == null || !date.isAfter(LocalDate.now());
    }

    @AssertTrue(message = "Ngày kết thúc không được sớm hơn ngày bắt đầu")
    public boolean isEndDateValid() {
        return startDate == null
                || endDate == null
                || !endDate.isBefore(startDate);
    }

    @AssertTrue(message = "Hộ đích không được để trống")
    public boolean isNewHouseholdRequired() {
        if (movementType == MovementType.TRANSFER
                || movementType == MovementType.MOVE_IN) {
            return newHouseholdId != null;
        }

        return true;
    }
}