package com.crishof.traveldeskapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tbl_payments")
public class Payment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Column(nullable = false, length = 10)
    private String sourceCurrency;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal exchangeRate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal convertedAmount;

    @Column(nullable = false)
    private Instant paymentDate;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        paymentDate = now;
    }
}
