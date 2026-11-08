package com.example.bluemoon.exception;

import com.example.bluemoon.response.ApiErrorResponse;
import com.example.bluemoon.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.LinkedHashMap;
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

    @ExceptionHandler(AppValidationException.class)
    public ResponseEntity<?> handleAppValidation(AppValidationException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("code", "VALIDATION_ERROR");
        body.put("message", ex.getMessage());
        if (ex.getErrors() != null && !ex.getErrors().isEmpty()) {
            body.put("errors", ex.getErrors());
        }
        return ResponseEntity.badRequest().body(body);
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

    @ExceptionHandler(HouseholdDuplicateException.class)
    public ResponseEntity<?> handleHouseholdDuplicate(
            HouseholdDuplicateException ex
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of(
                        "success", false,
                        "code", "DUPLICATE",
                        "message", "Dữ liệu đã tồn tại",
                        "errors", ex.getErrors()
                )
        );
    }

    @ExceptionHandler(InvalidNewHeadException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidNewHead(
            InvalidNewHeadException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiErrorResponse(
                        false,
                        "INVALID_NEW_HEAD",
                        ex.getMessage(),
                        Map.of("reason", ex.getReason())
                ));
    }

    @ExceptionHandler(HeadRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleHeadRequired(
            HeadRequiredException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiErrorResponse(
                        false,
                        "HEAD_REQUIRED",
                        ex.getMessage(),
                        Map.of("candidates", ex.getCandidates())
                ));
    }

    @ExceptionHandler(ResidentNotResidingException.class)
    public ResponseEntity<ApiErrorResponse> handleResidentNotResiding(
            ResidentNotResidingException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiErrorResponse(
                        false,
                        "RESIDENT_NOT_RESIDING",
                        ex.getMessage(),
                        Map.of("residencyStatus", ex.getResidencyStatus())
                ));
    }

    @ExceptionHandler(FeeCategoryValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleFeeCategoryValidation(
            FeeCategoryValidationException ex
    ) {
        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(
                        false,
                        "VALIDATION_ERROR",
                        "Dữ liệu khoản thu không hợp lệ",
                        Map.of("errors", ex.getErrors())
                )
        );
    }

    @ExceptionHandler(FeeCategoryDuplicateException.class)
    public ResponseEntity<ApiErrorResponse> handleFeeCategoryDuplicate(
            FeeCategoryDuplicateException ex
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse(
                        false,
                        "DUPLICATE",
                        ex.getMessage(),
                        Map.of(
                                "errors",
                                Map.of("code", "Mã khoản thu đã tồn tại")
                        )
                )
        );
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidTransition(
            InvalidTransitionException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiErrorResponse(
                        false,
                        "INVALID_TRANSITION",
                        ex.getMessage(),
                        Map.of(
                                "currentStatus", ex.getCurrentStatus(),
                                "allowedFrom", ex.getAllowedFrom()
                        )
                ));
    }

    @ExceptionHandler(HasTransactionsException.class)
    public ResponseEntity<ApiErrorResponse> handleHasTransactions(
            HasTransactionsException ex
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse(
                        false,
                        "HAS_TRANSACTIONS",
                        ex.getMessage(),
                        Map.of(
                                "errors", ex.getErrors(),
                                "suggestedAction", "CREATE_NEW"
                        )
                )
        );
    }

    @ExceptionHandler(HouseholdInactiveException.class)
    public ResponseEntity<ApiErrorResponse> handleHouseholdInactive(
            HouseholdInactiveException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiErrorResponse(
                        false,
                        "HOUSEHOLD_INACTIVE",
                        ex.getMessage(),
                        null
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatusException(
            ResponseStatusException ex
    ) {
        String message = ex.getReason() != null
                ? ex.getReason()
                : "Yêu cầu không hợp lệ";

        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "code", "NOT_FOUND",
                            "message", message
                    ));
        }

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(new ApiResponse<>(
                        false,
                        message,
                        null
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

    @ExceptionHandler(ResidentDeleteException.class)
    public ResponseEntity<ApiErrorResponse> handleResidentDeleteException(
            ResidentDeleteException ex
    ) {
        return ResponseEntity.status(ex.getStatus()).body(
                new ApiErrorResponse(
                        false,
                        ex.getCode(),
                        ex.getMessage(),
                        ex.getDetails()
                )
        );
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
