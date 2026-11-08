package com.example.bluemoon.dto;

import java.time.LocalDate;

public interface SearchResidentProjection {
    Long getId();
    String getFullName();
    LocalDate getDateOfBirth();
    Long getHouseholdId();
    String getHouseholdCode();
    String getApartmentNo();
    Boolean getIsHead();
    String getResidencyStatus();
    LocalDate getStatusUntil();
}
