package com.crishof.traveldeskapi.repository;

import com.crishof.traveldeskapi.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    List<Supplier> findAllByAgencyIdOrderByNameAsc(UUID agencyId);

    Optional<Supplier> findByIdAndAgencyId(UUID id, UUID agencyId);

    boolean existsByAgencyIdAndEmailIgnoreCase(UUID agencyId, String email);

    boolean existsByAgencyIdAndEmailIgnoreCaseAndIdNot(UUID agencyId, String email, UUID id);

    long countByAgencyId(UUID agencyId);
}
