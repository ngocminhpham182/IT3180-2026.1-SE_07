package com.example.bluemoon.repository;

import com.example.bluemoon.model.FeeCategory;
import com.example.bluemoon.model.enums.FeeKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeeCategoryRepository
        extends JpaRepository<FeeCategory, Long> {

    Optional<FeeCategory> findById(Long id);

    boolean existsByCode(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsById(Long Id);

    @Query("""
    SELECT f
    FROM FeeCategory f
    WHERE (:kind IS NULL OR f.feeKind = :kind)
      AND (:active IS NULL OR f.active = :active)
      AND (
          :keyword = ''
          OR LOWER(f.code) LIKE CONCAT('%', LOWER(:keyword), '%')
          OR LOWER(f.name) LIKE CONCAT('%', LOWER(:keyword), '%')
      )
    ORDER BY
        CASE
            WHEN f.feeKind = com.example.bluemoon.model.enums.FeeKind.MANDATORY
            THEN 0
            ELSE 1
        END,
        f.code ASC
""")
    List<FeeCategory> search(
            @Param("kind") FeeKind kind,
            @Param("active") Boolean active,
            @Param("keyword") String keyword
    );
}