package com.example.bluemoon.repository;


import com.example.bluemoon.model.Resident;
import com.example.bluemoon.model.enums.ResidencyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
    boolean existsByIdentityNumberAndDeletedAtIsNull(String identityNumber);
    Optional<Resident> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByIdentityNumberAndDeletedAtIsNullAndIdNot(
            String identityNumber,
            Long id
    );

    Optional<Resident> findByHouseholdIdAndHeadTrueAndDeletedAtIsNull(Long householdId);


    List<Resident> findByHousehold_IdAndDeletedAtIsNull(
            Long householdId,
            Sort sort
    );

    List<Resident> findByHousehold_IdAndDeletedAtIsNullAndStatusIn(
            Long householdId,
            Collection<ResidencyStatus> statuses,
            Sort sort
    );

    @Query(
            value = """
        SELECT r.*
        FROM resident r
        WHERE r.deleted_at IS NULL
          AND (
              CAST(:keyword AS TEXT) IS NULL
              OR f_unaccent(LOWER(r.full_name))
                    LIKE '%' || f_unaccent(LOWER(CAST(:keyword AS TEXT))) || '%'
          )
          AND (
              CAST(:householdId AS BIGINT) IS NULL
              OR r.household_id = :householdId
          )
          AND r.residency_status IN (:statuses)
          AND (
              :statusExpired = FALSE
              OR (
                  r.residency_status IN ('TEMPORARILY_ABSENT', 'TEMPORARY_RESIDENCE')
                  AND r.status_until < CURRENT_DATE
              )
          )
        ORDER BY r.full_name ASC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM resident r
        WHERE r.deleted_at IS NULL
          AND (
              CAST(:keyword AS TEXT) IS NULL
              OR f_unaccent(LOWER(r.full_name))
                    LIKE '%' || f_unaccent(LOWER(CAST(:keyword AS TEXT))) || '%'
          )
          AND (
              CAST(:householdId AS BIGINT) IS NULL
              OR r.household_id = :householdId
          )
          AND r.residency_status IN (:statuses)
          AND (
              :statusExpired = FALSE
              OR (
                  r.residency_status IN ('TEMPORARILY_ABSENT', 'TEMPORARY_RESIDENCE')
                  AND r.status_until < CURRENT_DATE
              )
          )
        """,
            nativeQuery = true
    )
    Page<Resident> searchResidents(
            @Param("keyword") String keyword,
            @Param("householdId") Long householdId,
            @Param("statuses") List<String> statuses,
            @Param("statusExpired") boolean statusExpired,
            Pageable pageable
    );
}