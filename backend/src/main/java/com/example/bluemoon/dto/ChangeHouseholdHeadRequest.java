package com.example.bluemoon.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeHouseholdHeadRequest(
        @NotNull(message = "residentId không được để trống")
        Long residentId
) {
}