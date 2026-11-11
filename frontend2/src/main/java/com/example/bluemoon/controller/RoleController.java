package com.example.bluemoon.controller;

import com.example.bluemoon.dto.RoleResponse;
import com.example.bluemoon.response.ApiResponse;
import com.example.bluemoon.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "OK",
                        userService.getRoles()
                )
        );
    }
}
