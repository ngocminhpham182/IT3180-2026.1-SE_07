package com.example.bluemoon.repository;

import com.example.bluemoon.model.ResidentRequest;
import com.example.bluemoon.model.enums.ResidentRequestStatus;
import com.example.bluemoon.model.enums.ResidentRequestType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResidentRequestRepository
        extends JpaRepository<ResidentRequest, Long>, JpaSpecificationExecutor<ResidentRequest> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ResidentRequest r WHERE r.id = :id")
    Optional<ResidentRequest> findByIdForUpdate(@Param("id") Long id);

    List<ResidentRequest> findByHousehold_IdAndStatus(Long householdId, ResidentRequestStatus status);

    Optional<ResidentRequest> findFirstByResident_IdAndTypeAndStatus(
            Long residentId,
            ResidentRequestType type,
            ResidentRequestStatus status
    );

    @Query(
            value = "SELECT id FROM resident_request WHERE status = 'PENDING' AND request_type = 'TEMPORARY_RESIDENCE' AND (proposed #>> '{person,identityNumber}') = :identityNumber LIMIT 1",
            nativeQuery = true
    )
    Optional<Long> findPendingTemporaryResidenceIdByIdentityNumber(@Param("identityNumber") String identityNumber);
}
