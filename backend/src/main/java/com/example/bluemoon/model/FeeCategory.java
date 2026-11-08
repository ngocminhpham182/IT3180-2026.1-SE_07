package com.example.bluemoon.model;

import com.example.bluemoon.model.enums.BillingCycle;
import com.example.bluemoon.model.enums.CalculationMethod;
import com.example.bluemoon.model.enums.FeeKind;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "fee_category",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fee_category_code", columnNames = "code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 30, unique = true)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_kind", nullable = false, length = 20)
    private FeeKind feeKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", nullable = false, length = 20)
    private CalculationMethod calculationMethod;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Column(name = "due_day")
    private Integer dueDay;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}