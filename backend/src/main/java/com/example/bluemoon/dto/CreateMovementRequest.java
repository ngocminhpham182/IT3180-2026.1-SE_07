package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.MovementType;

import java.time.LocalDate;

public record CreateMovementRequest(
        MovementType movementType,
        LocalDate effectiveDate,
        LocalDate startDate,
        LocalDate endDate,
        Long newHouseholdId,
        String relationshipToHead,
        Long newHeadResidentId,
        String note
) {}
