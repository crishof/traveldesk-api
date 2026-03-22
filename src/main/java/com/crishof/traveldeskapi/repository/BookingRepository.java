package com.crishof.traveldeskapi.repository;

import com.crishof.traveldeskapi.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking> findAllByAgencyIdOrderByCreatedAtDesc(UUID agencyId);

    Optional<Booking> findByIdAndAgencyId(UUID id, UUID agencyId);

    boolean existsByAgencyIdAndReferenceIgnoreCase(UUID agencyId, String reference);

    boolean existsByAgencyIdAndReferenceIgnoreCaseAndIdNot(UUID agencyId, String reference, UUID id);

    boolean existsByCustomerId(UUID customerId);

    boolean existsBySupplierId(UUID supplierId);

    long countByAgencyId(UUID agencyId);

    List<Booking> findAllByAgencyIdAndCustomerIdAndCreatedByIdAndDepartureDateAndStatus(
            UUID agencyId,
            UUID customerId,
            UUID createdById,
            java.time.LocalDate departureDate,
            com.crishof.traveldeskapi.model.BookingStatus status
    );
}
