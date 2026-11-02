package com.example.bluemoon.exception;


import com.example.bluemoon.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleInvalidRequest(
            HttpMessageNotReadableException ex
    ) {
        return ResponseEntity.badRequest().body(
                Map.of(
                        "success", false,
                        "code", "VALIDATION_ERROR",
                        "message", "Dữ liệu gửi lên không hợp lệ"
                )
        );
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<?> handleDuplicate(
            DuplicateException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "success", false,
                        "code", "DUPLICATE",
                        "message", "Dữ liệu đã tồn tại",
                        "errors", Map.of(
                                ex.getField(),
                                ex.getMessage()
                        )
                ));
    }

    @ExceptionHandler(SelfLockoutException.class)
    public ResponseEntity<?> handleSelfLockout(
            SelfLockoutException ex
    ) {
        return ResponseEntity
                .unprocessableEntity()
                .body(Map.of(
                        "success", false,
                        "code", "SELF_LOCKOUT",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(
            ResourceNotFoundException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "success", false,
                        "code", "NOT_FOUND",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(PasswordValidationException.class)
    public ResponseEntity<?> handlePasswordValidation(
            PasswordValidationException ex
    ) {

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "success", false,
                        "code", "VALIDATION_ERROR",
                        "message", "Dữ liệu không hợp lệ",
                        "errors", Map.of(
                                ex.getField(),
                                ex.getMessage()
                        )
                ));
    }

    @ExceptionHandler(SelfResetNotAllowedException.class)
    public ResponseEntity<?> handleSelfResetNotAllowed(
            SelfResetNotAllowedException ex
    ) {

        return ResponseEntity
                .unprocessableEntity()
                .body(Map.of(
                        "success", false,
                        "code", "SELF_RESET_NOT_ALLOWED",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(ResidentLinkRequiredException.class)
    public ResponseEntity<?> handleResidentLinkRequired(
            ResidentLinkRequiredException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of(
                        "success", false,
                        "code", "RESIDENT_LINK_REQUIRED",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<?> handleInvalidCredentials(
            InvalidCredentialsException ex
    ) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "success", false,
                        "code", "INVALID_CREDENTIALS",
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(
            MethodArgumentNotValidException ex
    ) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "success", false,
                        "code", "VALIDATION_ERROR",
                        "message", "Dữ liệu không hợp lệ",
                        "errors", errors
                ));
    }

}