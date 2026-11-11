package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.Gender;
import com.example.bluemoon.model.enums.ResidencyStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ResidentResponse(
        Long id,
        Long householdId,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        String identityNumber,
        String phone,
        String relationshipToHead,
        boolean isHead,
        ResidencyStatus residencyStatus,
        LocalDate statusUntil,
        LocalDate moveInDate,
        LocalDate moveOutDate,
        Instant createdAt,
        Instant updatedAt
) {}