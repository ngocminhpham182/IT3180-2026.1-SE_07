package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.ResidentRequestStatus;
import com.example.bluemoon.model.enums.ResidentRequestType;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ResidentRequestResponse(
        Long id,
        ResidentRequestType type,
        ResidentRequestStatus status,
        HouseholdRef household,
        ResidentRef resident,
        UserRef submittedBy,
        Instant submittedAt,
        String note,
        Map<String, Object> proposed,
        Map<String, Object> before,
        List<String> changes,
        DecisionRef decision,
        ResultRef result
) {
    public record HouseholdRef(
            Long id,
            String householdCode,
            String apartmentNo
    ) {}

    public record ResidentRef(
            Long id,
            String fullName
    ) {}

    public record UserRef(
            Long id,
            String fullName
    ) {}

    public record DecisionRef(
            ResidentRequestStatus outcome,
            UserRef decidedBy,
            Instant decidedAt,
            String reason,
            String note
    ) {}

    public record ResultRef(
            Long residentId,
            Long movementId
    ) {}
}
