package com.example.bluemoon.dto;


public record SkippedReceivable(
        Long householdId,
        String householdCode,
        String apartmentNo,
        Long feeCategoryId,
        String feeCode,
        String reason
) {}

