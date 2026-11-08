package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.ResidencyStatus;

import java.time.LocalDate;
import java.util.List;

public record SearchResponse(
        boolean empty,
        List<SearchHouseholdItem> households,
        List<SearchResidentItem> residents,
        SearchTotals totals,
        SearchHasMore hasMore
) {
    public record SearchHouseholdItem(
            Long id,
            String householdCode,
            String apartmentNo,
            Short floor,
            String status,
            String headName
    ) {}

    public record SearchResidentItem(
            Long id,
            String fullName,
            LocalDate dateOfBirth,
            Long householdId,
            String householdCode,
            String apartmentNo,
            boolean isHead,
            ResidencyStatus residencyStatus,
            LocalDate statusUntil,
            boolean statusExpired
    ) {}

    public record SearchTotals(
            long households,
            long residents
    ) {}

    public record SearchHasMore(
            boolean households,
            boolean residents
    ) {}
}
