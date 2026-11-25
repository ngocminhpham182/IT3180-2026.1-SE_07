package com.example.bluemoon.dto;

import java.util.List;

public record ReceivablePageResponse(
        List<ReceivableResponse> content,
        ReceivableSummaryResponse summary,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
