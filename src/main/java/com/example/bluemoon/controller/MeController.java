package com.example.bluemoon.controller;

import com.example.bluemoon.dto.ChangePasswordRequest;
import com.example.bluemoon.dto.UpdateProfileRequest;
import com.example.bluemoon.dto.UserResponse;
import com.example.bluemoon.response.ApiResponse;
import com.example.bluemoon.service.MeService;
import com.example.bluemoon.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {

        UserResponse response = userService.getMe();

        return ResponseEntity.ok(
                ApiResponse.success("OK", response)
        );
    }

    private final MeService meService;

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {

        UserResponse response =
                meService.updateProfile(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Cập nhật hồ sơ thành công",
                        response
                )
        );
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {

        meService.changePassword(request);

        return ResponseEntity.noContent().build();
    }
}