package com.crishof.traveldeskapi.repository;

import com.crishof.traveldeskapi.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findAllByAgencyIdOrderByFullNameAsc(UUID agencyId);

    Optional<Customer> findByIdAndAgencyId(UUID id, UUID agencyId);

    boolean existsByAgencyIdAndEmailIgnoreCase(UUID agencyId, String email);

    boolean existsByAgencyIdAndEmailIgnoreCaseAndIdNot(UUID agencyId, String email, UUID id);

    long countByAgencyId(UUID agencyId);
}
