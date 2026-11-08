package com.example.bluemoon.controller;

import com.example.bluemoon.dto.*;
import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.response.ApiResponse;
import com.example.bluemoon.service.ResidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentService residentService;

    @PostMapping("households/{id}/residents")
    public ResponseEntity<ApiResponse<ResidentResponse>> createResident(
            @PathVariable Long id,
            @Valid @RequestBody CreateResidentRequest request,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        ResidentResponse response = residentService.createResident(
                id, request, currentUser
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Thêm nhân khẩu thành công", response)
        );
    }

    @GetMapping("households/{id}/residents")
    public ResponseEntity<ApiResponse<List<ResidentInHouseholdResponse>>> getResidents(
            @PathVariable Long id,
            @RequestParam(required = false) String status
    ) {
        List<ResidentInHouseholdResponse> residents =
                residentService.getResidentsByHousehold(id, status);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách nhân khẩu thành công", residents)
        );
    }

    @GetMapping("residents")
    public ResponseEntity<ApiResponse<PageResponse<ResidentResponse>>> searchResidents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long householdId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean statusExpired,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ResidentResponse> response =
                residentService.searchResidents(
                        keyword,
                        householdId,
                        status,
                        statusExpired,
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.success("Lấy danh sách nhân khẩu thành công", response)
        );
    }

    @GetMapping("residents/{id}")
    public ResponseEntity<ApiResponse<ResidentResponse>> getResidentById(
            @PathVariable Long id
    ) {
        ResidentResponse response = residentService.getResidentById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy thông tin nhân khẩu thành công", response)
        );
    }

    @PutMapping("residents/{id}")
    public ResponseEntity<ApiResponse<ResidentResponse>> updateResident(
            @PathVariable Long id,
            @Valid @RequestBody UpdateResidentRequest request
    ) {
        ResidentResponse response = residentService.updateResident(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật thông tin nhân khẩu thành công", response)
        );
    }

    @DeleteMapping("residents/{id}")
    public ResponseEntity<Void> deleteResident(@PathVariable Long id) {
        residentService.deleteResident(id);
        return ResponseEntity.noContent().build();
    }
}