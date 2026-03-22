package com.crishof.traveldeskapi.dto;

import com.crishof.traveldeskapi.model.Currency;
import com.crishof.traveldeskapi.model.MovementType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountMovementDTO {

    private UUID id;

    private LocalDate date;

    private MovementType type;

    private String concept;

    private BigDecimal amount;

    private Currency currency;

    private UUID saleId;

}