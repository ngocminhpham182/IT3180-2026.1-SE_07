package com.example.bluemoon.controller;


import com.example.bluemoon.dto.BillingGenerationResponse;
import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.response.ApiResponse;
import com.example.bluemoon.service.BillingGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billing-periods")
@RequiredArgsConstructor
public class BillingGenerationController {

    private final BillingGenerationService billingGenerationService;

    @PostMapping("/{id}/generate")
    public ResponseEntity<ApiResponse<BillingGenerationResponse>> generate(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean dryRun,
            @AuthenticationPrincipal AppUser currentUser
    ) {
        BillingGenerationResponse response =
                billingGenerationService.generate(
                        id,
                        dryRun,
                        currentUser
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Lập danh sách khoản thu thành công",
                        response
                )
        );
    }
}

