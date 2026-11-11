package com.example.bluemoon.controller;

import com.example.bluemoon.dto.*;
import com.example.bluemoon.model.enums.HouseholdStatus;
import com.example.bluemoon.response.ApiResponse;
import com.example.bluemoon.service.HouseholdService;
import com.example.bluemoon.model.AppUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/households")
@RequiredArgsConstructor
public class HouseholdController {

    private final HouseholdService householdService;

    @PostMapping
    public ResponseEntity<?> createHousehold(
            @Valid @RequestBody CreateHouseholdRequest request,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        HouseholdResponse response =
                householdService.createHousehold(request, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of(
                        "success", true,
                        "message", "Tạo hộ gia đình thành công",
                        "data", response
                )
        );
    }

    @GetMapping
    public ResponseEntity<?> getHouseholds(
            @RequestParam(required = false) String keyword,

            @RequestParam(defaultValue = "ALL")
            HouseholdStatus status,

            @RequestParam(defaultValue = "1")
            @Min(1) int page,

            @RequestParam(defaultValue = "20")
            @Min(1) int size
    ) {
        PageResponse response =
                householdService.getHouseholds(keyword, status, page, size);

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Lấy danh sách hộ gia đình thành công",
                        "data", response
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getHouseholdById(
            @PathVariable Long id
    ) {
        HouseholdDetailResponse response =
                householdService.getHouseholdById(id);

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message", "Lấy thông tin hộ gia đình thành công",
                        "data", response
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HouseholdResponse>> updateHousehold(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHouseholdRequest request
    ) {
        HouseholdResponse response =
                householdService.updateHousehold(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Cập nhật hộ gia đình thành công",
                        response
                )
        );
    }

    @PutMapping("/{id}/head")
    public ResponseEntity<ApiResponse<HouseholdResponse>> changeHouseholdHead(
            @PathVariable Long id,
            @Valid @RequestBody ChangeHouseholdHeadRequest request
    ) {
        HouseholdResponse response = householdService.changeHouseholdHead(
                id,
                request.residentId()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Thay đổi chủ hộ thành công",
                        response
                )
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<HouseholdResponse>> updateHouseholdStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHouseholdStatusRequest request
    ) {
        HouseholdResponse response =
                householdService.updateHouseholdStatus(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Cập nhật trạng thái hộ gia đình thành công",
                        response
                )
        );
    }
}