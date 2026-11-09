
package com.example.bluemoon.controller;

import com.example.bluemoon.response.ApiResponse;
import com.example.bluemoon.dto.FeeCalculationPreviewRequest;
import com.example.bluemoon.dto.FeeCalculationPreviewResponse;
import com.example.bluemoon.service.FeeCalculationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fee-calculation")
@RequiredArgsConstructor
public class FeeCalculationController {

    private final FeeCalculationService feeCalculationService;

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<FeeCalculationPreviewResponse>> preview(
            @Valid @RequestBody FeeCalculationPreviewRequest request
    ) {
        FeeCalculationPreviewResponse response =
                feeCalculationService.preview(request);

        return ResponseEntity.ok(
                ApiResponse.success("OK", response)
        );
    }
}
