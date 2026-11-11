
package com.example.bluemoon.service;

import com.example.bluemoon.dto.FeeCalculationPreviewRequest;
import com.example.bluemoon.dto.FeeCalculationPreviewResponse;
import com.example.bluemoon.exception.FeeInactiveException;
import com.example.bluemoon.exception.FeeNotBillableException;
import com.example.bluemoon.exception.HouseholdInactiveException;
import com.example.bluemoon.exception.MissingAreaException;
import com.example.bluemoon.exception.ResourceNotFoundException;
import com.example.bluemoon.model.FeeCategory;
import com.example.bluemoon.model.Household;
import com.example.bluemoon.model.enums.CalculationMethod;
import com.example.bluemoon.model.enums.FeeKind;
import com.example.bluemoon.repository.FeeCategoryRepository;
import com.example.bluemoon.repository.HouseholdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class FeeCalculationService {

    private final HouseholdRepository householdRepository;
    private final FeeCategoryRepository feeCategoryRepository;

    @Transactional(readOnly = true)
    public FeeCalculationPreviewResponse preview(
            FeeCalculationPreviewRequest request
    ) {
        // 1. Tìm hộ gia đình, loại trừ hộ đã xóa mềm
        Household household = householdRepository
                .findByIdAndDeletedAtIsNull(request.householdId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy hộ gia đình"
                        )
                );

        // 2. Kiểm tra trạng thái hộ
        if (!"ACTIVE".equals(household.getStatus())) {
            throw new HouseholdInactiveException();
        }

        // 3. Tìm khoản thu
        FeeCategory feeCategory = feeCategoryRepository
                .findById(request.feeCategoryId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Không tìm thấy khoản thu"
                        )
                );

        // 4. Kiểm tra khoản thu còn hoạt động
        if (!feeCategory.isActive()) {
            throw new FeeInactiveException();
        }

        // 5. Không tính công nợ cho khoản tự nguyện
        if (feeCategory.getFeeKind() == FeeKind.VOLUNTARY) {
            throw new FeeNotBillableException();
        }

        BigDecimal unitPrice = feeCategory.getUnitPrice();

        BigDecimal area = null;
        BigDecimal amount;
        String formula;

        // 6. Tính tiền theo phương thức
        if (feeCategory.getCalculationMethod() == CalculationMethod.PER_AREA) {

            area = household.getArea();

            if (area == null) {
                throw new MissingAreaException(household.getId());
            }

            amount = area
                    .multiply(unitPrice)
                    .setScale(0, RoundingMode.HALF_UP);

            formula = format(area) + " x " + format(unitPrice);

        } else if (feeCategory.getCalculationMethod() == CalculationMethod.FIXED) {

            amount = unitPrice.setScale(0, RoundingMode.HALF_UP);

            formula = format(unitPrice);

        } else {
            // MANUAL không hỗ trợ tính tự động trong Sprint 1
            throw new FeeNotBillableException();
        }

        // 7. Trả kết quả xem trước, không lưu database
        return new FeeCalculationPreviewResponse(
                household.getId(),
                household.getHouseholdCode(),
                household.getApartmentNo(),

                feeCategory.getId(),
                feeCategory.getCode(),
                feeCategory.getName(),

                feeCategory.getCalculationMethod(),

                area,
                unitPrice,
                amount,
                formula
        );
    }

    private String format(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
