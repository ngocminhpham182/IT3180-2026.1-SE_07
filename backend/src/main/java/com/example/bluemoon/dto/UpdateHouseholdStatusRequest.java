package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.HouseholdStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateHouseholdStatusRequest(

        @NotNull(message = "status không được để trống")
        HouseholdStatus status

) {
}