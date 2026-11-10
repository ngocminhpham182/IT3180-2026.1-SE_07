package com.example.bluemoon.service;


import com.example.bluemoon.dto.BillingGenerationResponse;
import com.example.bluemoon.dto.FeeGenerationAggregate;
import com.example.bluemoon.dto.FeeGenerationSummary;
import com.example.bluemoon.dto.SkippedReceivable;
import com.example.bluemoon.exception.*;
import com.example.bluemoon.model.AppUser;
import com.example.bluemoon.model.BillingPeriod;
import com.example.bluemoon.model.enums.BillingPeriodStatus;
import com.example.bluemoon.repository.BillingPeriodRepository;
import com.example.bluemoon.repository.ReceivableRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingGenerationService {

    private final BillingPeriodRepository billingPeriodRepository;
    private final ReceivableRepository receivableRepository;

    @Transactional
    public BillingGenerationResponse generate(
            Long billingPeriodId,
            boolean dryRun,
            AppUser currentUser
    ) {
        // 1. Khóa kỳ thu
        BillingPeriod period = billingPeriodRepository
                .findByIdForUpdate(billingPeriodId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Không tìm thấy kỳ thu"));

        // 2. Kiểm tra kỳ đã đóng chưa
        if (period.getStatus() == BillingPeriodStatus.CLOSED) {
            throw new BillingPeriodClosedException();
        }

        // 3. Kiểm tra kỳ đã được lập chưa
        if (period.getGeneratedAt() != null) {
            throw new BillingPeriodAlreadyGeneratedException(
                    period.getGeneratedAt()
            );
        }

        // 4. Khóa các khoản mandatory active
        // Giúp tuần tự hóa việc xét ONE_TIME giữa các kỳ khác nhau.
        receivableRepository.lockActiveMandatoryFeeCategories();

        // 5. Kiểm tra có khoản thu đủ điều kiện
        if (receivableRepository.countEligibleFees() == 0) {
            throw new NoActiveFeeException();
        }

        // 6. Lấy thống kê trước khi INSERT
        List<FeeGenerationAggregate> aggregates =
                receivableRepository.getGenerationAggregates();

        List<SkippedReceivable> skipped =
                receivableRepository.getSkippedReceivables()
                        .stream()
                        .map(item -> new SkippedReceivable(
                                item.getHouseholdId(),
                                item.getHouseholdCode(),
                                item.getApartmentNo(),
                                item.getFeeCategoryId(),
                                item.getFeeCode(),
                                "MISSING_AREA"
                        ))
                        .toList();

        long generatedCount = aggregates.stream()
                .mapToLong(FeeGenerationAggregate::getReceivableCount)
                .sum();

        // 7. Không có receivable nào có thể sinh
        if (generatedCount == 0) {
            throw new NothingToGenerateException(skipped);
        }

        long householdCount =
                receivableRepository.countHouseholdsToGenerate();

        long inactiveHouseholdCount =
                receivableRepository.countInactiveHouseholds();

        BigDecimal totalAmount = aggregates.stream()
                .map(FeeGenerationAggregate::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<FeeGenerationSummary> byFee = aggregates.stream()
                .map(item -> new FeeGenerationSummary(
                        item.getFeeCategoryId(),
                        item.getFeeCode(),
                        item.getFeeName(),
                        item.getReceivableCount(),
                        item.getTotalAmount()
                ))
                .toList();

        // 8. Dry run: chỉ trả kết quả tính toán
        if (dryRun) {
            return new BillingGenerationResponse(
                    period.getId(),
                    true,
                    generatedCount,
                    householdCount,
                    totalAmount,
                    byFee,
                    skipped,
                    inactiveHouseholdCount
            );
        }

        // 9. Generate thật: INSERT hàng loạt
        int insertedCount = receivableRepository.generateReceivables(
                period.getId(),
                currentUser.getId()
        );

        // Đảm bảo số lượng INSERT khớp với số đã tính
        if (insertedCount != generatedCount) {
            throw new IllegalStateException(
                    "Số receivable được lập không khớp với kết quả tính toán"
            );
        }

        // 10. Đánh dấu kỳ đã được lập
        period.setGeneratedAt(Instant.now());
        period.setGeneratedBy(currentUser);

        billingPeriodRepository.save(period);

        return new BillingGenerationResponse(
                period.getId(),
                false,
                generatedCount,
                householdCount,
                totalAmount,
                byFee,
                skipped,
                inactiveHouseholdCount
        );
    }
}
