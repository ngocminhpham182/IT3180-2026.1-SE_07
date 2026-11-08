
package com.example.bluemoon.model;

import com.example.bluemoon.model.enums.BillingPeriodStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "billing_period")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Builder.Default
    private BillingPeriodStatus status = BillingPeriodStatus.OPEN;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private AppUser generatedBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private AppUser closedBy;
}
