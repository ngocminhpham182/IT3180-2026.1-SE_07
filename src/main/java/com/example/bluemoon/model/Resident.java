package com.example.bluemoon.model;

import com.example.bluemoon.model.enums.ResidencyStatus;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "household_id")
    private Long householdId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ResidencyStatus status;
}