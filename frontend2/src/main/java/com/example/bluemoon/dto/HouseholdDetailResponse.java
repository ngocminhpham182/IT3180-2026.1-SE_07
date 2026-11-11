package com.example.bluemoon.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@AllArgsConstructor
public class HouseholdDetailResponse {
    private Long id;
    private String householdCode;
    private String apartmentNo;
    private Short floor;
    private BigDecimal area;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ResidentInHouseholdResponse> residents;
}