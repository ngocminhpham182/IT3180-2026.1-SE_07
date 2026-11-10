package com.example.bluemoon.repository;

import com.example.bluemoon.model.BillingPeriod;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface BillingPeriodRepository
        extends JpaRepository<BillingPeriod, Long>,
        JpaSpecificationExecutor<BillingPeriod> {

    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);

    // Giữ lại method kiểm tra overlap đã dùng ở API POST
    Optional<BillingPeriod>
    findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            java.time.LocalDate endDate,
            java.time.LocalDate startDate
    );

    @EntityGraph(attributePaths = {"generatedBy", "closedBy"})
    Optional<BillingPeriod> findWithUsersById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT bp
    FROM BillingPeriod bp
    WHERE bp.id = :id
    """)
    Optional<BillingPeriod> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        SELECT bp
        FROM BillingPeriod bp
        WHERE bp.id <> :id
          AND bp.startDate <= :endDate
          AND bp.endDate >= :startDate
        """)
    Optional<BillingPeriod> findOverlappingPeriod(
            @Param("id") Long id,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}