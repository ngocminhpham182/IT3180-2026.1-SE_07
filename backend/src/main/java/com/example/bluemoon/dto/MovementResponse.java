package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.MovementType;

import java.time.Instant;
import java.time.LocalDate;

public record MovementResponse(
        Long id,
        Long residentId,
        MovementType movementType,
        LocalDate effectiveDate,
        LocalDate startDate,
        LocalDate endDate,
        HouseholdRef oldHousehold,
        HouseholdRef newHousehold,
        String note,
        boolean isInitial,
        UserRef recordedBy,
        Instant recordedAt
) {
    public record HouseholdRef(
            Long id,
            String householdCode,
            String apartmentNo
    ) {}

    public record UserRef(
            Long id,
            String fullName
    ) {}
}
