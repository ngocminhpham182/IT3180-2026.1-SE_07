package com.example.bluemoon.controller;

import com.example.bluemoon.dto.*;
import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.response.ApiResponse;
import com.example.bluemoon.service.ResidentRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class ResidentRequestController {

    private final ResidentRequestService residentRequestService;

    @PostMapping("me/resident-requests")
    public ResponseEntity<ApiResponse<ResidentRequestResponse>> submitRequest(
            @RequestBody SubmitResidentRequest request,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        ResidentRequestResponse response = residentRequestService.submit(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Đã gửi đề nghị, đang chờ Ban quản trị duyệt", response)
        );
    }

    @GetMapping("me/resident-requests")
    public ResponseEntity<ApiResponse<PageResponse<ResidentRequestResponse>>> listMyRequests(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        PageResponse<ResidentRequestResponse> response = residentRequestService.listMine(
                currentUser, status, type, page, size
        );
        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }

    @GetMapping("me/resident-requests/{id}")
    public ResponseEntity<ApiResponse<ResidentRequestResponse>> getMyRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        ResidentRequestResponse response = residentRequestService.getMine(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }

    @PostMapping("me/resident-requests/{id}/cancel")
    public ResponseEntity<ApiResponse<ResidentRequestResponse>> cancelMyRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        ResidentRequestResponse response = residentRequestService.cancel(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Đã hủy đề nghị", response));
    }

    @GetMapping("resident-requests")
    public ResponseEntity<ApiResponse<PageResponse<ResidentRequestResponse>>> listAllRequests(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long householdId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ResidentRequestResponse> response = residentRequestService.listAll(
                status, type, householdId, keyword, page, size
        );
        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }

    @GetMapping("resident-requests/{id}")
    public ResponseEntity<ApiResponse<ResidentRequestResponse>> getRequestById(
            @PathVariable Long id
    ) {
        ResidentRequestResponse response = residentRequestService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }

    @PostMapping("resident-requests/{id}/approve")
    public ResponseEntity<ApiResponse<ResidentRequestResponse>> approveRequest(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ApproveRequest request,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        ResidentRequestResponse response = residentRequestService.approve(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Đã duyệt đề nghị", response));
    }

    @PostMapping("resident-requests/{id}/reject")
    public ResponseEntity<ApiResponse<ResidentRequestResponse>> rejectRequest(
            @PathVariable Long id,
            @Valid @RequestBody RejectRequest request,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        ResidentRequestResponse response = residentRequestService.reject(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối đề nghị", response));
    }
}
