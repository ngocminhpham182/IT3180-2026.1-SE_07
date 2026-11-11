package com.example.bluemoon.model;

import com.example.bluemoon.model.enums.BillingPeriodStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "billing_period")
@Getter
@Setter
@NoArgsConstructor
public class BillingPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BillingPeriodStatus status = BillingPeriodStatus.OPEN;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "generated_by",
            foreignKey = @ForeignKey(name = "billing_period_generated_by_fkey")
    )
    private AppUser generatedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "closed_by",
            foreignKey = @ForeignKey(name = "billing_period_closed_by_fkey")
    )
    private AppUser closedBy;
}