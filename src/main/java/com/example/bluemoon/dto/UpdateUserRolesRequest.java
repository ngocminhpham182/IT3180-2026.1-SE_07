package com.example.bluemoon.dto;

import com.example.bluemoon.model.enums.RoleCode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateUserRolesRequest(

        @NotNull(message = "Roles không được để trống")
        @NotEmpty(message = "Phải có ít nhất một vai trò")
        List<RoleCode> roles

) {
}
