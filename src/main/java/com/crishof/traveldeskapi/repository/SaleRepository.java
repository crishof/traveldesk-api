package com.crishof.traveldeskapi.repository;

import com.crishof.traveldeskapi.model.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SaleRepository extends JpaRepository<Sale, UUID> {

    List<Sale> findAllByAgencyIdOrderBySaleDateDesc(UUID agencyId);

    Optional<Sale> findByIdAndAgencyId(UUID id, UUID agencyId);

    boolean existsByCustomerId(UUID customerId);

    long countByAgencyId(UUID agencyId);

    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.payments WHERE s.id = :id AND s.agency.id = :agencyId")
    Optional<Sale> findByIdAndAgencyIdWithPayments(UUID id, UUID agencyId);

    List<Sale> findByCreatedById(UUID createdById);

    List<Sale> findByCreatedByIdAndCurrencyOrderBySaleDateAsc(UUID createdById, String currency);

}
