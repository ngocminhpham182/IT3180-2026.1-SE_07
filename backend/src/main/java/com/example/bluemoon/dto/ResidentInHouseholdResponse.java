package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.Gender;
import com.example.bluemoon.model.enums.ResidencyStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ResidentInHouseholdResponse {
    private Long id;
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String identityNumber;
    private String phone;
    private String relationshipToHead;

    @JsonProperty("isHead")
    private boolean isHead;

    @JsonProperty("residencyStatus")
    private ResidencyStatus residencyStatus;

    private LocalDate statusUntil;
    private LocalDate moveInDate;
    private LocalDate moveOutDate;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isHead() {
        return isHead;
    }

    public ResidencyStatus getResidencyStatus() {
        return residencyStatus;
    }
}
