package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.ResidentRequestType;

import java.math.BigDecimal;
import java.util.List;

public record MyHouseholdResponse(
        HouseholdInfo household,
        Long myResidentId,
        List<ResidentInHouseholdResponse> residents,
        List<PendingRequestItem> pendingRequests
) {
    public record HouseholdInfo(
            Long id,
            String householdCode,
            String apartmentNo,
            Short floor,
            BigDecimal area,
            String status
    ) {}

    public record PendingRequestItem(
            Long id,
            ResidentRequestType type,
            Long residentId
    ) {}
}
