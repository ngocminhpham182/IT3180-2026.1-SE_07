package com.example.bluemoon.exception;

import com.example.bluemoon.response.ApiErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRequest(
            HttpMessageNotReadableException ex
    ) {
        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(
                        false,
                        "VALIDATION_ERROR",
                        "Dữ liệu gửi lên không hợp lệ"
                )
        );
    }

    @ExceptionHandler(AppValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleAppValidation(AppValidationException ex) {
        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(
                        false,
                        "VALIDATION_ERROR",
                        ex.getMessage(),
                        ex.getErrors(),
                        null
                )
        );
    }

    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(
            DuplicateException ex
    ) {
        Map<String, String> errors = ex.getField() != null
                ? Map.of(ex.getField(), ex.getMessage())
                : Collections.emptyMap();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        false,
                        "DUPLICATE",
                        "Dữ liệu đã tồn tại",
                        errors,
                        null
                ));
    }

    @ExceptionHandler(HouseholdDuplicateException.class)
    public ResponseEntity<ApiErrorResponse> handleHouseholdDuplicate(
            HouseholdDuplicateException ex
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse(
                        false,
                        "DUPLICATE",
                        "Dữ liệu đã tồn tại",
                        ex.getErrors(),
                        null
                )
        );
    }

    @ExceptionHandler(FeeCategoryDuplicateException.class)
    public ResponseEntity<ApiErrorResponse> handleFeeCategoryDuplicate(
            FeeCategoryDuplicateException ex
    ) {
        Map<String, String> errors = Map.of("code", "Mã khoản thu đã tồn tại");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse(
                        false,
                        "DUPLICATE",
                        ex.getMessage(),
                        errors,
                        Map.of("errors", errors)
                )
        );
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
                        ex.getErrors(),
                        Map.of("errors", ex.getErrors())
                )
        );
    }

    @ExceptionHandler(PasswordValidationException.class)
    public ResponseEntity<ApiErrorResponse> handlePasswordValidation(
            PasswordValidationException ex
    ) {
        Map<String, String> errors = ex.getField() != null
                ? Map.of(ex.getField(), ex.getMessage())
                : Collections.emptyMap();

        return ResponseEntity
                .badRequest()
                .body(new ApiErrorResponse(
                        false,
                        "VALIDATION_ERROR",
                        "Dữ liệu không hợp lệ",
                        errors,
                        null
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error -> {
                    String field = error.getField();
                    // Ánh xạ tên method @AssertTrue sang field tương ứng để UI bắt đúng input
                    switch (field) {
                        case "dateOfBirthValid", "dateOfBirthInRange" -> field = "dateOfBirth";
                        case "startDateValid", "temporaryDatesValid" -> field = "startDate";
                        case "endDateValid" -> field = "endDate";
                    }
                    errors.putIfAbsent(field, error.getDefaultMessage());
                });

        return ResponseEntity
                .badRequest()
                .body(new ApiErrorResponse(
                        false,
                        "VALIDATION_ERROR",
                        "Dữ liệu không hợp lệ",
                        errors,
                        null
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> cv : ex.getConstraintViolations()) {
            String property = cv.getPropertyPath().toString();
            int lastDot = property.lastIndexOf('.');
            if (lastDot >= 0) {
                property = property.substring(lastDot + 1);
            }
            errors.putIfAbsent(property, cv.getMessage());
        }

        return ResponseEntity
                .badRequest()
                .body(new ApiErrorResponse(
                        false,
                        "VALIDATION_ERROR",
                        "Dữ liệu không hợp lệ",
                        errors,
                        null
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex
    ) {
        return ResponseEntity
                .badRequest()
                .body(new ApiErrorResponse(
                        false,
                        "VALIDATION_ERROR",
                        ex.getMessage() != null ? ex.getMessage() : "Dữ liệu không hợp lệ"
                ));
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
                        null,
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
                        null,
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
                        null,
                        Map.of("residencyStatus", ex.getResidencyStatus())
                ));
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
                        null,
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
                        ex.getErrors(),
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
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(
            ResponseStatusException ex
    ) {
        String message = ex.getReason() != null
                ? ex.getReason()
                : "Yêu cầu không hợp lệ";

        String code = ex.getStatusCode() == HttpStatus.NOT_FOUND
                ? "NOT_FOUND"
                : "VALIDATION_ERROR";

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(new ApiErrorResponse(
                        false,
                        code,
                        message
                ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(
                        false,
                        "NOT_FOUND",
                        ex.getMessage()
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
                        null,
                        ex.getDetails()
                )
        );
    }

    @ExceptionHandler(SelfResetNotAllowedException.class)
    public ResponseEntity<ApiErrorResponse> handleSelfResetNotAllowed(
            SelfResetNotAllowedException ex
    ) {
        return ResponseEntity
                .unprocessableEntity()
                .body(new ApiErrorResponse(
                        false,
                        "SELF_RESET_NOT_ALLOWED",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(SelfLockoutException.class)
    public ResponseEntity<ApiErrorResponse> handleSelfLockout(
            SelfLockoutException ex
    ) {
        return ResponseEntity
                .unprocessableEntity()
                .body(new ApiErrorResponse(
                        false,
                        "SELF_LOCKOUT",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(ResidentLinkRequiredException.class)
    public ResponseEntity<ApiErrorResponse> handleResidentLinkRequired(
            ResidentLinkRequiredException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiErrorResponse(
                        false,
                        "RESIDENT_LINK_REQUIRED",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(
                        false,
                        "INVALID_CREDENTIALS",
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(MissingAreaException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingArea(
            MissingAreaException ex
    ) {
        return ResponseEntity.unprocessableEntity().body(
                new ApiErrorResponse(
                        false,
                        "MISSING_AREA",
                        ex.getMessage(),
                        null,
                        Map.of("householdId", ex.getHouseholdId())
                )
        );
    }

    @ExceptionHandler(FeeInactiveException.class)
    public ResponseEntity<ApiErrorResponse> handleFeeInactive(
            FeeInactiveException ex
    ) {
        return ResponseEntity.unprocessableEntity().body(
                new ApiErrorResponse(
                        false,
                        "FEE_INACTIVE",
                        ex.getMessage()
                )
        );
    }

    @ExceptionHandler(FeeNotBillableException.class)
    public ResponseEntity<ApiErrorResponse> handleFeeNotBillable(
            FeeNotBillableException ex
    ) {
        return ResponseEntity.unprocessableEntity().body(
                new ApiErrorResponse(
                        false,
                        "FEE_NOT_BILLABLE",
                        ex.getMessage()
                )
        );
    }

    @ExceptionHandler(InvalidBillingPeriodDateRangeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidBillingPeriodDateRange(
            InvalidBillingPeriodDateRangeException ex
    ) {
        Map<String, String> errors = Map.of(
                "endDate",
                "Ngày kết thúc không được sớm hơn ngày bắt đầu"
        );
        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(
                        false,
                        "VALIDATION_ERROR",
                        "Dữ liệu không hợp lệ",
                        errors,
                        Map.of("endDate", "Ngày kết thúc không được sớm hơn ngày bắt đầu")
                )
        );
    }

    @ExceptionHandler(BillingPeriodOverlapException.class)
    public ResponseEntity<ApiErrorResponse> handleBillingPeriodOverlap(
            BillingPeriodOverlapException ex
    ) {
        return ResponseEntity.unprocessableEntity().body(
                new ApiErrorResponse(
                        false,
                        "PERIOD_OVERLAP",
                        ex.getMessage(),
                        null,
                        Map.of(
                                "overlapping",
                                Map.of(
                                        "id", ex.getOverlappingId(),
                                        "code", ex.getOverlappingCode()
                                )
                        )
                )
        );
    }

    @ExceptionHandler(BillingPeriodClosedException.class)
    public ResponseEntity<ApiErrorResponse> handlePeriodClosed(
            BillingPeriodClosedException ex) {
        return ResponseEntity.unprocessableEntity().body(
                new ApiErrorResponse(false, "PERIOD_CLOSED",
                        ex.getMessage())
        );
    }

    @ExceptionHandler(BillingPeriodAlreadyGeneratedException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyGenerated(
            BillingPeriodAlreadyGeneratedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse(false, "ALREADY_GENERATED",
                        ex.getMessage(),
                        null,
                        Map.of("generatedAt", ex.getGeneratedAt()))
        );
    }

    @ExceptionHandler(NoActiveFeeException.class)
    public ResponseEntity<ApiErrorResponse> handleNoActiveFee(
            NoActiveFeeException ex) {
        return ResponseEntity.unprocessableEntity().body(
                new ApiErrorResponse(false, "NO_ACTIVE_FEE",
                        ex.getMessage())
        );
    }

    @ExceptionHandler(NothingToGenerateException.class)
    public ResponseEntity<ApiErrorResponse> handleNothingToGenerate(
            NothingToGenerateException ex) {
        return ResponseEntity.unprocessableEntity().body(
                new ApiErrorResponse(false, "NOTHING_TO_GENERATE",
                        ex.getMessage(),
                        null,
                        Map.of("skipped", ex.getSkipped()))
        );
    }

    @ExceptionHandler(PeriodNotGeneratedException.class)
    public ResponseEntity<ApiErrorResponse> handlePeriodNotGenerated(
            PeriodNotGeneratedException ex
    ) {
        return ResponseEntity.unprocessableEntity().body(
                new ApiErrorResponse(
                        false,
                        "PERIOD_NOT_GENERATED",
                        ex.getMessage()
                )
        );
    }

    @ExceptionHandler(ResidentNotInHouseholdException.class)
    public ResponseEntity<ApiErrorResponse> handleResidentNotInHousehold(ResidentNotInHouseholdException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ApiErrorResponse(
                        false,
                        "RESIDENT_NOT_IN_HOUSEHOLD",
                        ex.getMessage() != null ? ex.getMessage() : "Nhân khẩu không thuộc hộ gia đình của bạn"
                )
        );
    }

    @ExceptionHandler(PendingRequestExistsException.class)
    public ResponseEntity<ApiErrorResponse> handlePendingRequestExists(PendingRequestExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse(
                        false,
                        "PENDING_REQUEST_EXISTS",
                        ex.getMessage(),
                        null,
                        ex.getRequestId() != null ? Map.of("requestId", ex.getRequestId()) : null
                )
        );
    }

    @ExceptionHandler(RequestNotPendingException.class)
    public ResponseEntity<ApiErrorResponse> handleRequestNotPending(RequestNotPendingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse(
                        false,
                        "REQUEST_NOT_PENDING",
                        ex.getMessage(),
                        null,
                        ex.getStatus() != null ? Map.of("status", ex.getStatus().name()) : null
                )
        );
    }

    @ExceptionHandler(RequestOutdatedException.class)
    public ResponseEntity<ApiErrorResponse> handleRequestOutdated(RequestOutdatedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ApiErrorResponse(
                        false,
                        "REQUEST_OUTDATED",
                        ex.getMessage(),
                        null,
                        Map.of("fields", ex.getFields() != null ? ex.getFields() : List.of())
                )
        );
    }

    @ExceptionHandler(NoChangesException.class)
    public ResponseEntity<ApiErrorResponse> handleNoChanges(NoChangesException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                new ApiErrorResponse(
                        false,
                        "NO_CHANGES",
                        ex.getMessage()
                )
        );
    }
}
