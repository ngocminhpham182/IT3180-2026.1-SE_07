package com.example.bluemoon.repository;

import com.example.bluemoon.model.BillingPeriod;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface BillingPeriodRepository
        extends JpaRepository<BillingPeriod, Long>,
        JpaSpecificationExecutor<BillingPeriod> {

    boolean existsByCode(String code);

    // Giữ lại method kiểm tra overlap đã dùng ở API POST
    Optional<BillingPeriod>
    findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(
            java.time.LocalDate endDate,
            java.time.LocalDate startDate
    );

    @EntityGraph(attributePaths = {"generatedBy", "closedBy"})
    Optional<BillingPeriod> findWithUsersById(Long id);
}