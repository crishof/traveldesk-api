package com.crishof.traveldeskapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "account_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    private LocalDate date;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Currency currency;

    private String description;
}