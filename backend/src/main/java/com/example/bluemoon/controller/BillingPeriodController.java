package com.example.bluemoon.controller;

import com.example.bluemoon.dto.BillingPeriodDetailResponse;
import com.example.bluemoon.dto.PageResponse;
import com.example.bluemoon.model.enums.BillingPeriodStatus;
import com.example.bluemoon.response.ApiResponse;
import com.example.bluemoon.dto.BillingPeriodRequest;
import com.example.bluemoon.dto.BillingPeriodResponse;
import com.example.bluemoon.service.BillingPeriodService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billing-periods")
@RequiredArgsConstructor
public class BillingPeriodController {

    private final BillingPeriodService billingPeriodService;

    @PostMapping
    public ResponseEntity<ApiResponse<BillingPeriodResponse>> create(
            @Valid @RequestBody BillingPeriodRequest request
    ) {
        BillingPeriodResponse response = billingPeriodService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo kỳ thu thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<BillingPeriodResponse>>> getAll(
            @RequestParam(required = false) Integer year,

            @RequestParam(required = false) BillingPeriodStatus status,

            @RequestParam(required = false) Boolean generated,

            @RequestParam(defaultValue = "1")
            @Min(value = 1, message = "page phải lớn hơn hoặc bằng 1")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size phải lớn hơn 0")
            @Max(value = 100, message = "size không được vượt quá 100")
            int size
    ) {
        PageResponse<BillingPeriodResponse> response =
                billingPeriodService.getAll(
                        year, status, generated, page, size
                );

        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách kỳ thu thành công", response)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BillingPeriodDetailResponse>> getById(
            @PathVariable Long id
    ) {
        BillingPeriodDetailResponse response =
                billingPeriodService.getById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy chi tiết kỳ thu thành công", response)
        );
    }
}