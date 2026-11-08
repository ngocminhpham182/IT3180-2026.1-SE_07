package com.example.bluemoon.repository;

import com.example.bluemoon.model.Receivable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivableRepository extends JpaRepository<Receivable, Long> {

    boolean existsByFeeCategory_Id(Long feeCategoryId);
}
