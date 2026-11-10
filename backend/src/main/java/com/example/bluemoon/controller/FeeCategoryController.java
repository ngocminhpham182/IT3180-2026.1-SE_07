package com.example.bluemoon.controller;

import com.example.bluemoon.dto.UpdateFeeCategoryStatusRequest;
import com.example.bluemoon.model.enums.FeeKind;
import com.example.bluemoon.response.ApiResponse;
import com.example.bluemoon.dto.CreateFeeCategoryRequest;
import com.example.bluemoon.dto.FeeCategoryResponse;
import com.example.bluemoon.service.FeeCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/fee-categories")
@RequiredArgsConstructor
public class FeeCategoryController {

    private final FeeCategoryService feeCategoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<FeeCategoryResponse>> create(
            @Valid @RequestBody CreateFeeCategoryRequest request
    ) {
        FeeCategoryResponse response = feeCategoryService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Tạo danh mục khoản thu thành công",
                        response
                ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeeCategoryResponse>>> getAll(
            @RequestParam(required = false) FeeKind kind,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String keyword
    ) {
        System.out.println(">>> GET FEE CATEGORIES CONTROLLER");

        List<FeeCategoryResponse> response =
                feeCategoryService.getAll(kind, active, keyword);

        return ResponseEntity.ok(ApiResponse.success("OK", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeCategoryResponse>> getById(
            @PathVariable Long id
    ) {
        FeeCategoryResponse response = feeCategoryService.getById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Lấy thông tin khoản thu thành công", response)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FeeCategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateFeeCategoryRequest request
    ) {
        FeeCategoryResponse response = feeCategoryService.update(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật khoản thu thành công", response)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FeeCategoryResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFeeCategoryStatusRequest request
    ) {
        FeeCategoryResponse response =
                feeCategoryService.updateStatus(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật trạng thái khoản thu thành công", response)
        );
    }
}
