package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.MovementType;

import java.time.Instant;
import java.time.LocalDate;

public record ResidentMovementResponse(
        Long id,
        Long residentId,
        MovementType movementType,
        Long oldHouseholdId,
        Long newHouseholdId,
        LocalDate effectiveDate,
        LocalDate startDate,
        LocalDate endDate,
        String note,
        Long recordedBy,
        Instant recordedAt
) {}