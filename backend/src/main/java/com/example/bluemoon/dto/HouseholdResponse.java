package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.Gender;
import com.example.bluemoon.model.enums.ResidencyStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

public record HouseholdResponse(
        Long id,
        String householdCode,
        String apartmentNo,
        Short floor,
        BigDecimal area,
        String status,
        Instant createdAt,
        Instant updatedAt,
        HeadResponse head,
        int residentCount,
        int presentCount
) {

    public record HeadResponse(
            Long id,
            String fullName,
            LocalDate dateOfBirth,
            Gender gender,
            String identityNumber,
            String phone,
            boolean isHead,
            ResidencyStatus residencyStatus
    ) {
    }
}