package com.crishof.traveldeskapi.repository;

import com.crishof.traveldeskapi.model.AccountPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountPaymentRepository extends JpaRepository<AccountPayment, UUID> {

    List<AccountPayment> findByUserId(UUID userId);

    List<AccountPayment> findByUserIdAndCurrencyOrderByDateAscIdAsc(UUID userId, com.crishof.traveldeskapi.model.Currency currency);

}