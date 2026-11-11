package com.example.bluemoon.controller;

import com.example.bluemoon.dto.ChangePasswordRequest;
import com.example.bluemoon.dto.MyHouseholdResponse;
import com.example.bluemoon.dto.UpdateProfileRequest;
import com.example.bluemoon.dto.UserResponse;
import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.response.ApiResponse;
import com.example.bluemoon.service.MeService;
import com.example.bluemoon.service.ResidentRequestService;
import com.example.bluemoon.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final MeService meService;
    private final ResidentRequestService residentRequestService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {

        UserResponse response = userService.getMe();

        return ResponseEntity.ok(
                ApiResponse.success("OK", response)
        );
    }

    @GetMapping("/household")
    public ResponseEntity<ApiResponse<MyHouseholdResponse>> getMyHousehold(
            @AuthenticationPrincipal AppUser currentUser
    ) {
        MyHouseholdResponse response = residentRequestService.getMyHousehold(currentUser);
        return ResponseEntity.ok(
                ApiResponse.success("OK", response)
        );
    }

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
