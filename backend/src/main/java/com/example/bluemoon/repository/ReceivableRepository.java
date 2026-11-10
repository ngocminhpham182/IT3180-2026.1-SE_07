package com.example.bluemoon.repository;

import com.example.bluemoon.dto.BillingPeriodSummaryResponse;
import com.example.bluemoon.model.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReceivableRepository extends JpaRepository<Receivable, Long> {

    boolean existsByFeeCategory_Id(Long feeCategoryId);

    @Query("""
            SELECT new com.example.bluemoon.dto.BillingPeriodSummaryResponse(
                COUNT(r.id),
                COUNT(DISTINCT r.household.id),
                COALESCE(SUM(r.amountDue), 0)
            )
            FROM Receivable r
            WHERE r.billingPeriod.id = :billingPeriodId
            """)
    Optional<BillingPeriodSummaryResponse> getSummaryByBillingPeriodId(
            @Param("billingPeriodId") Long billingPeriodId
    );
}
