package com.example.bluemoon.model;

import com.example.bluemoon.model.enums.MovementType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "resident_movement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResidentMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nhân khẩu có biến động
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resident_id", nullable = false)
    private Resident resident;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private MovementType movementType;

    // Hộ cũ, có thể null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_household_id")
    private Household oldHousehold;

    // Hộ mới, có thể null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_household_id")
    private Household newHousehold;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String note;

    // Tài khoản ghi nhận biến động
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by", nullable = false)
    private AppUser recordedBy;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Column(name = "is_initial", nullable = false)
    @Builder.Default
    private boolean isInitial = false;

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }
}