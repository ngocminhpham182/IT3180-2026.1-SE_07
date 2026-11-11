package com.example.bluemoon.controller;

import com.example.bluemoon.dto.*;
import com.example.bluemoon.response.ApiResponse;
import com.example.bluemoon.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {

        UserResponse response = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Tạo tài khoản thành công",
                                response
                        )
                );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(

            @RequestParam(required = false) String keyword,

            @RequestParam(required = false) String role,

            @RequestParam(required = false) Boolean active,

            @RequestParam(defaultValue = "1") int page,

            @RequestParam(defaultValue = "20") int size
    ) {

        PageResponse<UserResponse> response =
                userService.getUsers(
                        keyword,
                        role,
                        active,
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.success("OK", response)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long id
    ) {

        UserResponse response =
                userService.getUserById(id);

        return ResponseEntity.ok(
                ApiResponse.success("OK", response)
        );
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRolesRequest request
    ) {

        UserResponse response =
                userService.updateUserRoles(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Cập nhật vai trò thành công",
                        response
                )
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {

        UserResponse response =
                userService.updateUserStatus(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Cập nhật trạng thái tài khoản thành công",
                        response
                )
        );
    }

    @PostMapping("/{id}/password-reset")
    public ResponseEntity<ApiResponse<PasswordResetResponse>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request
    ) {

        PasswordResetResponse response =
                userService.resetPassword(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Đã đặt lại mật khẩu",
                        response
                )
        );
    }
}