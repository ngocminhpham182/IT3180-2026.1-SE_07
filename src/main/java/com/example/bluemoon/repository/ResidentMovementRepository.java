package com.example.bluemoon.repository;

import com.example.bluemoon.model.Resident;
import com.example.bluemoon.model.ResidentMovement;
import com.example.bluemoon.model.enums.ResidencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResidentMovementRepository
        extends JpaRepository<ResidentMovement, Long> {

    long countByResident_IdAndIsInitialFalse(Long residentId);

    List<ResidentMovement> findByResident_IdOrderByRecordedAtAsc(Long residentId);

    List<Resident> findByHousehold_IdAndDeletedAtIsNullAndStatusAndIdNot(
            Long householdId,
            ResidencyStatus status,
            Long residentId
    );
}