package com.example.bluemoon.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(

        @NotNull(message = "Trạng thái active không được để trống")
        Boolean active

) {
}
