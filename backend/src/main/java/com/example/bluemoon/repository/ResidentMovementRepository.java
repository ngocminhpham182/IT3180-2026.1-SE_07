package com.example.bluemoon.repository;

import com.example.bluemoon.model.ResidentMovement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResidentMovementRepository
        extends JpaRepository<ResidentMovement, Long> {

    long countByResident_IdAndIsInitialFalse(Long residentId);

    @EntityGraph(attributePaths = {"resident", "oldHousehold", "newHousehold", "recordedBy"})
    List<ResidentMovement> findByResident_IdOrderByEffectiveDateDescRecordedAtDescIdDesc(Long residentId);

    Optional<ResidentMovement> findFirstByResident_IdOrderByEffectiveDateDescRecordedAtDescIdDesc(Long residentId);
}
