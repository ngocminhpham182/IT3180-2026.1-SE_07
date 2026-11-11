package com.example.bluemoon.model;

import com.example.bluemoon.model.enums.Gender;
import com.example.bluemoon.model.enums.ResidencyStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "resident")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(name = "identity_number", length = 30)
    private String identityNumber;

    @Column(length = 20)
    private String phone;

    @Column(name = "relationship_to_head", length = 50)
    private String relationshipToHead;

    @Column(name = "is_head", nullable = false)
    @Builder.Default
    private boolean head = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "residency_status", nullable = false, length = 30)
    @Builder.Default
    private ResidencyStatus status = ResidencyStatus.RESIDING;

    @Column(name = "status_until")
    private LocalDate statusUntil;

    @Column(name = "move_in_date")
    private LocalDate moveInDate;

    @Column(name = "move_out_date")
    private LocalDate moveOutDate;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "resident")
    @Builder.Default
    private List<ResidentMovement> movements = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = ResidencyStatus.RESIDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}