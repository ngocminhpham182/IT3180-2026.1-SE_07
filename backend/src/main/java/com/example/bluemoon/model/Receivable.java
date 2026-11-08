
package com.example.bluemoon.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "receivable",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_receivable_period",
                        columnNames = {
                                "household_id",
                                "fee_category_id",
                                "billing_period_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_receivable_household",
                        columnList = "household_id"
                ),
                @Index(
                        name = "idx_receivable_period",
                        columnList = "billing_period_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receivable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Hộ gia đình phải thanh toán
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    // Loại khoản thu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_category_id", nullable = false)
    private FeeCategory feeCategory;

    // Kỳ thu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_period_id", nullable = false)
    private BillingPeriod billingPeriod;

    // Số lượng áp dụng, thường là diện tích đối với PER_AREA
    @Column(precision = 10, scale = 2)
    private BigDecimal quantity;

    // Đơn giá tại thời điểm lập danh sách
    @Column(
            name = "unit_price_applied",
            nullable = false,
            precision = 14,
            scale = 2
    )
    private BigDecimal unitPriceApplied;

    // Tổng số tiền phải thu tại thời điểm lập danh sách
    @Column(
            name = "amount_due",
            nullable = false,
            precision = 14,
            scale = 2
    )
    private BigDecimal amountDue;

    // Hạn thanh toán
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Người tạo khoản phải thu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Optimistic locking
    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
