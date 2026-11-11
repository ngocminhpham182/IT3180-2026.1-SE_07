package com.example.bluemoon.repository;

import com.example.bluemoon.dto.SearchResidentProjection;
import com.example.bluemoon.model.Resident;
import com.example.bluemoon.model.enums.ResidencyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
    boolean existsByIdentityNumberAndDeletedAtIsNull(String identityNumber);
    Optional<Resident> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Resident r WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<Resident> findByIdForUpdate(@Param("id") Long id);

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

    @Query("SELECT r FROM Resident r WHERE r.household.id = :householdId AND r.deletedAt IS NULL AND r.id <> :residentId AND r.status = com.example.bluemoon.model.enums.ResidencyStatus.RESIDING ORDER BY r.fullName ASC")
    List<Resident> findCandidatesForNewHead(@Param("householdId") Long householdId, @Param("residentId") Long residentId);

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

    @Query(
            value = """
        SELECT
            r.id AS id,
            r.full_name AS fullName,
            r.date_of_birth AS dateOfBirth,
            h.id AS householdId,
            h.household_code AS householdCode,
            h.apartment_no AS apartmentNo,
            r.is_head AS isHead,
            r.residency_status AS residencyStatus,
            r.status_until AS statusUntil
        FROM resident r
        JOIN household h ON r.household_id = h.id AND h.deleted_at IS NULL
        WHERE r.deleted_at IS NULL
          AND f_unaccent(LOWER(r.full_name)) LIKE '%' || f_unaccent(LOWER(CAST(:keyword AS TEXT))) || '%'
        ORDER BY r.full_name ASC
        LIMIT :limit
        """,
            nativeQuery = true
    )
    List<SearchResidentProjection> quickSearchResidents(
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );

    @Query(
            value = """
        SELECT COUNT(*)
        FROM resident r
        JOIN household h ON r.household_id = h.id AND h.deleted_at IS NULL
        WHERE r.deleted_at IS NULL
          AND f_unaccent(LOWER(r.full_name)) LIKE '%' || f_unaccent(LOWER(CAST(:keyword AS TEXT))) || '%'
        """,
            nativeQuery = true
    )
    long countQuickSearchResidents(@Param("keyword") String keyword);
}
