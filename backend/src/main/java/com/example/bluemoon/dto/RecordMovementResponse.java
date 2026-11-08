package com.example.bluemoon.dto;

public record RecordMovementResponse(
        MovementResponse movement,
        ResidentResponse resident
) {}
