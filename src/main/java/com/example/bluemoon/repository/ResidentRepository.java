package com.example.bluemoon.repository;


import com.example.bluemoon.model.Resident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentRepository extends JpaRepository<Resident, Long> {
}