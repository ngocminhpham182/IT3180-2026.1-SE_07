package com.example.bluemoon.repository;

import com.example.bluemoon.dto.BillingPeriodSummaryResponse;
import com.example.bluemoon.dto.FeeGenerationAggregate;
import com.example.bluemoon.dto.SkippedReceivableProjection;
import com.example.bluemoon.model.Receivable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ReceivableRepository extends JpaRepository<Receivable, Long> {

    boolean existsByFeeCategory_Id(Long feeCategoryId);

    @Query("SELECT DISTINCT r.feeCategory.id FROM Receivable r")
    List<Long> findDistinctFeeCategoryIds();

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

    @Query(value = """
    SELECT f.id
    FROM fee_category f
    WHERE f.fee_kind = 'MANDATORY'
      AND f.is_active = TRUE
    ORDER BY f.id
    FOR UPDATE
    """, nativeQuery = true)
    List<Long> lockActiveMandatoryFeeCategories();

    @Query(value = """
    SELECT COUNT(*)
    FROM fee_category f
    WHERE f.fee_kind = 'MANDATORY'
      AND f.is_active = TRUE
      AND (
          f.billing_cycle = 'MONTHLY'
          OR (
              f.billing_cycle = 'ONE_TIME'
              AND NOT EXISTS (
                  SELECT 1
                  FROM receivable r
                  WHERE r.fee_category_id = f.id
              )
          )
      )
    """, nativeQuery = true)
    long countEligibleFees();

    @Query(value = """
    WITH eligible_fee AS (
        SELECT f.*
        FROM fee_category f
        WHERE f.fee_kind = 'MANDATORY'
          AND f.is_active = TRUE
          AND (
              f.billing_cycle = 'MONTHLY'
              OR (
                  f.billing_cycle = 'ONE_TIME'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM receivable r
                      WHERE r.fee_category_id = f.id
                  )
              )
          )
    )
    SELECT
        f.id AS "feeCategoryId",
        f.code AS "feeCode",
        f.name AS "feeName",
        COUNT(*) AS "receivableCount",
        SUM(
            CASE
                WHEN f.calculation_method = 'PER_AREA'
                    THEN ROUND(h.area * f.unit_price, 0)
                WHEN f.calculation_method = 'FIXED'
                    THEN ROUND(f.unit_price, 0)
            END
        ) AS "totalAmount"
    FROM household h
    CROSS JOIN eligible_fee f
    WHERE h.status = 'ACTIVE'
      AND h.deleted_at IS NULL
      AND (
          (f.calculation_method = 'PER_AREA' AND h.area IS NOT NULL)
          OR f.calculation_method = 'FIXED'
      )
    GROUP BY f.id, f.code, f.name
    ORDER BY f.id
    """, nativeQuery = true)
    List<FeeGenerationAggregate> getGenerationAggregates();

    @Query(value = """
    WITH eligible_fee AS (
        SELECT f.*
        FROM fee_category f
        WHERE f.fee_kind = 'MANDATORY'
          AND f.is_active = TRUE
          AND (
              f.billing_cycle = 'MONTHLY'
              OR (
                  f.billing_cycle = 'ONE_TIME'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM receivable r
                      WHERE r.fee_category_id = f.id
                  )
              )
          )
    )
    SELECT
        h.id AS "householdId",
        h.household_code AS "householdCode",
        h.apartment_no AS "apartmentNo",
        f.id AS "feeCategoryId",
        f.code AS "feeCode"
    FROM household h
    CROSS JOIN eligible_fee f
    WHERE h.status = 'ACTIVE'
      AND h.deleted_at IS NULL
      AND f.calculation_method = 'PER_AREA'
      AND h.area IS NULL
    ORDER BY h.id, f.id
    """, nativeQuery = true)
    List<SkippedReceivableProjection> getSkippedReceivables();

    @Query(value = """
    SELECT COUNT(DISTINCT h.id)
    FROM household h
    WHERE h.status = 'ACTIVE'
      AND h.deleted_at IS NULL
      AND EXISTS (
          SELECT 1
          FROM fee_category f
          WHERE f.fee_kind = 'MANDATORY'
            AND f.is_active = TRUE
            AND (
                f.billing_cycle = 'MONTHLY'
                OR (
                    f.billing_cycle = 'ONE_TIME'
                    AND NOT EXISTS (
                        SELECT 1
                        FROM receivable r
                        WHERE r.fee_category_id = f.id
                    )
                )
            )
            AND (
                f.calculation_method = 'FIXED'
                OR (
                    f.calculation_method = 'PER_AREA'
                    AND h.area IS NOT NULL
                )
            )
      )
    """, nativeQuery = true)
    long countHouseholdsToGenerate();

    @Query(value = """
    SELECT COUNT(*)
    FROM household
    WHERE status = 'INACTIVE'
      AND deleted_at IS NULL
    """, nativeQuery = true)
    long countInactiveHouseholds();

    @Modifying
    @Query(value = """
    WITH eligible_fee AS (
        SELECT f.*
        FROM fee_category f
        WHERE f.fee_kind = 'MANDATORY'
          AND f.is_active = TRUE
          AND (
              f.billing_cycle = 'MONTHLY'
              OR (
                  f.billing_cycle = 'ONE_TIME'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM receivable r
                      WHERE r.fee_category_id = f.id
                  )
              )
          )
    )
    INSERT INTO receivable (
        household_id,
        fee_category_id,
        billing_period_id,
        quantity,
        unit_price_applied,
        amount_due,
        due_date,
        created_by
    )
    SELECT
        h.id,
        f.id,
        bp.id,
        CASE
            WHEN f.calculation_method = 'PER_AREA'
                THEN h.area
            ELSE NULL
        END,
        f.unit_price,
        CASE
            WHEN f.calculation_method = 'PER_AREA'
                THEN ROUND(h.area * f.unit_price, 0)
            WHEN f.calculation_method = 'FIXED'
                THEN ROUND(f.unit_price, 0)
        END,
        MAKE_DATE(
            EXTRACT(YEAR FROM bp.start_date)::INTEGER,
            EXTRACT(MONTH FROM bp.start_date)::INTEGER,
            f.due_day::INTEGER
        ),
        :createdBy
    FROM household h
    CROSS JOIN eligible_fee f
    JOIN billing_period bp ON bp.id = :billingPeriodId
    WHERE h.status = 'ACTIVE'
      AND h.deleted_at IS NULL
      AND (
          (f.calculation_method = 'PER_AREA' AND h.area IS NOT NULL)
          OR f.calculation_method = 'FIXED'
      )
    """, nativeQuery = true)
    int generateReceivables(
            @Param("billingPeriodId") Long billingPeriodId,
            @Param("createdBy") Long createdBy
    );

    @Query(
            value = """
            SELECT r
            FROM Receivable r
            JOIN FETCH r.household h
            JOIN FETCH r.feeCategory f
            WHERE r.billingPeriod.id = :billingPeriodId
              AND (:householdId IS NULL OR h.id = :householdId)
              AND (:feeCategoryId IS NULL OR f.id = :feeCategoryId)
              AND (
                  :keyword = ''
                  OR LOWER(h.householdCode) LIKE CONCAT('%', LOWER(:keyword), '%')
                  OR LOWER(h.apartmentNo) LIKE CONCAT('%', LOWER(:keyword), '%')
              )
            ORDER BY h.householdCode ASC, f.code ASC, r.id ASC
            """,
            countQuery = """
            SELECT COUNT(r)
            FROM Receivable r
            JOIN r.household h
            JOIN r.feeCategory f
            WHERE r.billingPeriod.id = :billingPeriodId
              AND (:householdId IS NULL OR h.id = :householdId)
              AND (:feeCategoryId IS NULL OR f.id = :feeCategoryId)
              AND (
                  :keyword = ''
                  OR LOWER(h.householdCode) LIKE CONCAT('%', LOWER(:keyword), '%')
                  OR LOWER(h.apartmentNo) LIKE CONCAT('%', LOWER(:keyword), '%')
              )
            """
    )
    Page<Receivable> searchReceivables(
            @Param("billingPeriodId") Long billingPeriodId,
            @Param("householdId") Long householdId,
            @Param("feeCategoryId") Long feeCategoryId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            SELECT COALESCE(SUM(r.amountDue), 0)
            FROM Receivable r
            JOIN r.household h
            JOIN r.feeCategory f
            WHERE r.billingPeriod.id = :billingPeriodId
              AND (:householdId IS NULL OR h.id = :householdId)
              AND (:feeCategoryId IS NULL OR f.id = :feeCategoryId)
              AND (
                  :keyword = ''
                  OR LOWER(h.householdCode) LIKE CONCAT('%', LOWER(:keyword), '%')
                  OR LOWER(h.apartmentNo) LIKE CONCAT('%', LOWER(:keyword), '%')
              )
            """)
    BigDecimal sumAmountDue(
            @Param("billingPeriodId") Long billingPeriodId,
            @Param("householdId") Long householdId,
            @Param("feeCategoryId") Long feeCategoryId,
            @Param("keyword") String keyword
    );
}
