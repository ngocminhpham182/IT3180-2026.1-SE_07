package com.example.bluemoon.service;

import com.example.bluemoon.dto.CreateFeeCategoryRequest;
import com.example.bluemoon.dto.FeeCategoryResponse;
import com.example.bluemoon.dto.UpdateFeeCategoryStatusRequest;
import com.example.bluemoon.exception.*;
import com.example.bluemoon.model.FeeCategory;
import com.example.bluemoon.model.enums.BillingCycle;
import com.example.bluemoon.model.enums.CalculationMethod;
import com.example.bluemoon.model.enums.FeeKind;
import com.example.bluemoon.repository.FeeCategoryRepository;
import com.example.bluemoon.repository.ReceivableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FeeCategoryService {

    private static final BigDecimal MAX_UNIT_PRICE =
            new BigDecimal("1000000000");

    private final FeeCategoryRepository feeCategoryRepository;
    private final ReceivableRepository receivableRepository;

    @Transactional
    public FeeCategoryResponse create(CreateFeeCategoryRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        // Chuẩn hóa dữ liệu đầu vào
        String code = request.code().trim().toUpperCase();
        String name = request.name().trim();

        if (name.isEmpty()) {
            errors.put("name", "Tên khoản thu không được để trống");
        }

        if (request.description() != null
                && request.description().length() > 1000) {
            errors.put("description", "Mô tả tối đa 1000 ký tự");
        }

        // Kiểm tra mã sau khi chuẩn hóa
        if (!code.matches("[A-Z0-9_]{2,30}")) {
            errors.put(
                    "code",
                    "Mã khoản thu chỉ được chứa chữ in hoa, chữ số và dấu gạch dưới"
            );
        }

        if (feeCategoryRepository.existsByCode(code)) {
            throw new FeeCategoryDuplicateException(
                    "Mã khoản thu đã tồn tại"
            );
        }

        FeeKind feeKind = request.feeKind();
        CalculationMethod method = request.calculationMethod();

        if (feeKind == null) {
            errors.put("feeKind", "Loại khoản thu không được để trống");
        }

        if (method == null) {
            errors.put("calculationMethod", "Phương thức tính không được để trống");
        }

        BigDecimal unitPrice = BigDecimal.ZERO;
        BillingCycle billingCycle = BillingCycle.ONE_TIME;
        Integer dueDay = null;

        if (feeKind == FeeKind.MANDATORY) {

            // MANDATORY chỉ hỗ trợ PER_AREA hoặc FIXED
            if (method != CalculationMethod.PER_AREA
                    && method != CalculationMethod.FIXED) {
                errors.put(
                        "calculationMethod",
                        "Khoản bắt buộc chỉ hỗ trợ PER_AREA hoặc FIXED"
                );
            }

            // Đơn giá bắt buộc > 0
            if (request.unitPrice() == null
                    || request.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                errors.put("unitPrice", "Đơn giá phải lớn hơn 0");
            } else {
                unitPrice = request.unitPrice();

                if (unitPrice.compareTo(MAX_UNIT_PRICE) > 0) {
                    errors.put(
                            "unitPrice",
                            "Đơn giá tối đa là 1.000.000.000"
                    );
                }

                if (unitPrice.scale() > 2) {
                    errors.put(
                            "unitPrice",
                            "Đơn giá tối đa 2 chữ số thập phân"
                    );
                }
            }

            // Chu kỳ thu bắt buộc
            if (request.billingCycle() == null) {
                errors.put("billingCycle", "Chu kỳ thu không được để trống");
            } else {
                billingCycle = request.billingCycle();
            }

            // Hạn nộp bắt buộc, từ 1 đến 28
            if (request.dueDay() == null) {
                errors.put("dueDay", "Hạn nộp không được để trống");
            } else if (request.dueDay() < 1 || request.dueDay() > 28) {
                errors.put("dueDay", "Hạn nộp phải từ 1 đến 28");
            } else {
                dueDay = request.dueDay();
            }

        } else if (feeKind == FeeKind.VOLUNTARY) {

            // Khoản tự nguyện chỉ dùng MANUAL
            if (method != CalculationMethod.MANUAL) {
                errors.put(
                        "calculationMethod",
                        "Khoản tự nguyện phải sử dụng MANUAL"
                );
            }

            // Các trường này bị bỏ qua, BE tự chuẩn hóa
            unitPrice = BigDecimal.ZERO;
            billingCycle = BillingCycle.ONE_TIME;
            dueDay = null;
        }

        if (!errors.isEmpty()) {
            throw new FeeCategoryValidationException(errors);
        }

        FeeCategory feeCategory = FeeCategory.builder()
                .code(code)
                .name(name)
                .description(
                        request.description() == null
                                ? null
                                : request.description().trim()
                )
                .feeKind(feeKind)
                .calculationMethod(method)
                .unitPrice(unitPrice)
                .billingCycle(billingCycle)
                .dueDay(dueDay)
                .active(true)
                .build();

        try {
            FeeCategory saved = feeCategoryRepository.save(feeCategory);
            return toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            // Chống trường hợp hai request đồng thời tạo cùng một code
            throw new FeeCategoryDuplicateException(
                    "Mã khoản thu đã tồn tại"
            );
        }
    }

    private FeeCategoryResponse toResponse(FeeCategory feeCategory) {
        return new FeeCategoryResponse(
                feeCategory.getId(),
                feeCategory.getCode(),
                feeCategory.getName(),
                feeCategory.getDescription(),
                feeCategory.getFeeKind(),
                feeCategory.getCalculationMethod(),
                feeCategory.getUnitPrice(),
                feeCategory.getBillingCycle(),
                feeCategory.getDueDay(),
                feeCategory.isActive(),
                feeCategory.getCreatedAt(),
                feeCategory.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<FeeCategoryResponse> getAll(
            FeeKind kind,
            Boolean active,
            String keyword
    ) {
        String normalizedKeyword =
                keyword == null ? "" : keyword.trim();

        return feeCategoryRepository
                .search(kind, active, normalizedKeyword)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FeeCategoryResponse getById(Long id) {
        FeeCategory feeCategory = feeCategoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy khoản thu")
                );

        return toResponse(feeCategory);
    }

    @Transactional
    public FeeCategoryResponse update(
            Long id,
            CreateFeeCategoryRequest request
    ) {
        FeeCategory category = feeCategoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy khoản thu")
                );

        // Chuẩn hóa code giống POST
        String normalizedCode = request.code() == null
                ? null
                : request.code().trim().toUpperCase();

        // Kiểm tra code trùng, kể cả khoản thu inactive.
        if (normalizedCode != null
                && feeCategoryRepository.existsByCodeIgnoreCaseAndIdNot(
                normalizedCode, id
        )) {
            throw new DuplicateException("code", "Mã khoản thu đã tồn tại");
        }

        // Kiểm tra khoản thu đã có công nợ hay chưa.
        boolean inUse = receivableRepository.existsByFeeCategory_Id(id);

        if (inUse) {
            Map<String, String> errors = new LinkedHashMap<>();

            if (!Objects.equals(category.getCode(), normalizedCode)) {
                errors.put(
                        "code",
                        "Không thể thay đổi mã khoản thu đã phát sinh công nợ"
                );
            }

            if (category.getFeeKind() != request.feeKind()) {
                errors.put(
                        "feeKind",
                        "Không thể thay đổi loại khoản thu đã phát sinh công nợ"
                );
            }

            if (category.getCalculationMethod() != request.calculationMethod()) {
                errors.put(
                        "calculationMethod",
                        "Không thể thay đổi phương thức tính đã phát sinh công nợ"
                );
            }

            // VOLUNTARY luôn được backend chuẩn hóa thành ONE_TIME.
            BillingCycle newBillingCycle =
                    request.feeKind() == FeeKind.VOLUNTARY
                            ? BillingCycle.ONE_TIME
                            : request.billingCycle();

            if (category.getBillingCycle() != newBillingCycle) {
                errors.put(
                        "billingCycle",
                        "Không thể thay đổi chu kỳ thu đã phát sinh công nợ"
                );
            }

            if (!errors.isEmpty()) {
                throw new HasTransactionsException(errors);
            }
        }

        // Cập nhật các trường được phép.
        category.setCode(normalizedCode);
        category.setName(request.name().trim());
        category.setDescription(request.description());
        category.setFeeKind(request.feeKind());
        category.setCalculationMethod(request.calculationMethod());

        if (request.feeKind() == FeeKind.MANDATORY) {
            category.setUnitPrice(request.unitPrice());
            category.setBillingCycle(request.billingCycle());
            category.setDueDay(request.dueDay());
        } else {
            category.setUnitPrice(BigDecimal.ZERO);
            category.setBillingCycle(BillingCycle.ONE_TIME);
            category.setDueDay(null);
        }

        FeeCategory saved = feeCategoryRepository.save(category);

        return toResponse(saved);
    }

    @Transactional
    public FeeCategoryResponse updateStatus(
            Long id,
            UpdateFeeCategoryStatusRequest request
    ) {
        FeeCategory category = feeCategoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy khoản thu")
                );

        category.setActive(request.active());

        FeeCategory saved = feeCategoryRepository.save(category);

        return toResponse(saved);
    }
}