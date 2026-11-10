package com.example.bluemoon.repository;

import com.example.bluemoon.model.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByResidentId(Long residentId);

    boolean existsByEmailIgnoreCaseAndIdNot(
            String email,
            Long id
    );

    Page<AppUser> findAll(Specification<AppUser> specification, Pageable pageable);
}
