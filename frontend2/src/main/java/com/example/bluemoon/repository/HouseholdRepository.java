package com.example.bluemoon.repository;

import com.example.bluemoon.dto.HouseholdListItemResponse;
import com.example.bluemoon.model.Household;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HouseholdRepository extends JpaRepository<Household, Long> {

    boolean existsByHouseholdCodeAndDeletedAtIsNull(String householdCode);

    boolean existsByApartmentNoAndDeletedAtIsNull(String apartmentNo);

    boolean existsByHouseholdCodeAndDeletedAtIsNullAndIdNot(
            String householdCode,
            Long id
    );

    boolean existsByApartmentNoAndDeletedAtIsNullAndIdNot(
            String apartmentNo,
            Long id
    );

    @EntityGraph(attributePaths = "residents")
    Optional<Household> findByIdAndDeletedAtIsNull(Long id);
    @Query(
            value = """
        SELECT
            h.id AS id,
            h.household_code AS householdCode,
            h.apartment_no AS apartmentNo,
            h.floor AS floor,
            h.status AS status,
            r.full_name AS headName
        FROM household h
        LEFT JOIN resident r
            ON r.household_id = h.id
            AND r.is_head = true
            AND r.deleted_at IS NULL
        WHERE h.deleted_at IS NULL
          AND (
                CAST(:keyword AS TEXT) IS NULL
                OR LOWER(h.household_code) LIKE
                   '%' || LOWER(CAST(:keyword AS TEXT)) || '%'
                OR LOWER(h.apartment_no) LIKE
                   '%' || LOWER(CAST(:keyword AS TEXT)) || '%'
          )
          AND (
                CAST(:status AS TEXT) = 'ALL'
                OR h.status = CAST(:status AS TEXT)
          )
        ORDER BY h.household_code ASC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM household h
        WHERE h.deleted_at IS NULL
          AND (
                CAST(:keyword AS TEXT) IS NULL
                OR LOWER(h.household_code) LIKE
                   '%' || LOWER(CAST(:keyword AS TEXT)) || '%'
                OR LOWER(h.apartment_no) LIKE
                   '%' || LOWER(CAST(:keyword AS TEXT)) || '%'
          )
          AND (
                CAST(:status AS TEXT) = 'ALL'
                OR h.status = CAST(:status AS TEXT)
          )
        """,
            nativeQuery = true
    )
    Page<HouseholdListItemResponse> searchHouseholds(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );
}