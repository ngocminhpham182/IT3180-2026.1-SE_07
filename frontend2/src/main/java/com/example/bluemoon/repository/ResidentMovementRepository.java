package com.example.bluemoon.repository;

import com.example.bluemoon.model.ResidentMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentMovementRepository
        extends JpaRepository<ResidentMovement, Long> {

    long countByResident_IdAndIsInitialFalse(Long residentId);
}