package com.example.bluemoon.dto;

import java.math.BigDecimal;

public record ReceivableSummaryResponse(
        BigDecimal totalDue
) {
}
