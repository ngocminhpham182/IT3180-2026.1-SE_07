package com.example.bluemoon.dto;

public record CreateResidentMovementResponse(
        ResidentMovementResponse movement,
        ResidentResponse resident
) {}