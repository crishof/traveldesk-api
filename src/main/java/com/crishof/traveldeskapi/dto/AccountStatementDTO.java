package com.crishof.traveldeskapi.dto;

import com.crishof.traveldeskapi.model.Currency;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountStatementDTO {

    private Currency currency;

    private BigDecimal balance;

    private List<AccountMovementDTO> movements;

}