package com.example.bluemoon.repository;


import com.example.bluemoon.model.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
    boolean existsByIdentityNumberAndDeletedAtIsNull(String identityNumber);
    Optional<Resident> findByIdAndDeletedAtIsNull(Long id);

    Optional<Resident> findByHouseholdIdAndHeadTrueAndDeletedAtIsNull(Long householdId);
}