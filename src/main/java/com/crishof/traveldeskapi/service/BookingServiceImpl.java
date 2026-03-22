package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.BookingRequest;
import com.crishof.traveldeskapi.dto.BookingResponse;
import com.crishof.traveldeskapi.exception.ConflictException;
import com.crishof.traveldeskapi.exception.InvalidRequestException;
import com.crishof.traveldeskapi.exception.ResourceNotFoundException;
import com.crishof.traveldeskapi.model.*;
import com.crishof.traveldeskapi.model.agency.Agency;
import com.crishof.traveldeskapi.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final AgencyRepository agencyRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<BookingResponse> getAll(UUID agencyId) {
        validateAgencyId(agencyId);

        return bookingRepository.findAllByAgencyIdOrderByCreatedAtDesc(agencyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public BookingResponse create(UUID agencyId, UUID userId, BookingRequest request) {
        validateAgencyId(agencyId);
        validateUserId(userId);
        validateDateRange(request.departureDate(), request.returnDate());

        Agency agency = getAgencyOrThrow(agencyId);
        Customer customer = getCustomerOrThrow(agencyId, request.customerId());
        Supplier supplier = getSupplierOrNull(agencyId, request.supplierId());
        User createdBy = getUserOrThrow(userId, agencyId);

        String normalizedReference = normalizeReference(request.reference());
        BookingStatus status = parseBookingStatus(request.status());

        validateReferenceUniqueness(agencyId, normalizedReference);

        Booking booking = new Booking();
        booking.setAgency(agency);
        booking.setCustomer(customer);
        booking.setSupplier(supplier);
        booking.setCreatedBy(createdBy);
        booking.setReference(normalizedReference);
        booking.setDescription(normalizeText(request.destination()));
        booking.setAmount(request.amount());
        String normalizedCurrency = normalizeText(request.currency()).toUpperCase(Locale.ROOT);
        booking.setCurrency(normalizedCurrency);
        BigDecimal originalAmount = request.originalAmount() != null ? request.originalAmount() : request.amount();
        String sourceCurrency = request.sourceCurrency() != null
            ? normalizeText(request.sourceCurrency()).toUpperCase(Locale.ROOT)
            : normalizedCurrency;
        BigDecimal exchangeRate = request.exchangeRate() != null ? request.exchangeRate() : BigDecimal.ONE;
        BigDecimal convertedAmount = request.convertedAmount() != null
            ? request.convertedAmount()
            : originalAmount.multiply(exchangeRate);
        booking.setOriginalAmount(originalAmount);
        booking.setSourceCurrency(sourceCurrency);
        booking.setExchangeRate(exchangeRate);
        booking.setConvertedAmount(convertedAmount);
        booking.setDepartureDate(request.departureDate());
        booking.setReturnDate(request.returnDate());
        booking.setPaymentDate(request.paymentDate());
        booking.setStatus(status);

        return toResponse(bookingRepository.save(booking));
    }

    @Override
    public BookingResponse update(UUID agencyId, UUID id, BookingRequest request) {
        validateAgencyId(agencyId);
        validateDateRange(request.departureDate(), request.returnDate());

        Booking booking = getBookingOrThrow(agencyId, id);
        Customer customer = getCustomerOrThrow(agencyId, request.customerId());
        Supplier supplier = getSupplierOrNull(agencyId, request.supplierId());

        String normalizedReference = normalizeReference(request.reference());
        String normalizedDestination = normalizeText(request.destination());
        BookingStatus status = parseBookingStatus(request.status());

        validateReferenceUniquenessForUpdate(agencyId, normalizedReference, id);

        booking.setCustomer(customer);
        booking.setSupplier(supplier);
        booking.setReference(normalizedReference);
        booking.setDescription(normalizedDestination);
        booking.setAmount(request.amount());
        String normalizedCurrency = normalizeText(request.currency()).toUpperCase(Locale.ROOT);
        booking.setCurrency(normalizedCurrency);
        BigDecimal originalAmount = request.originalAmount() != null ? request.originalAmount() : request.amount();
        String sourceCurrency = request.sourceCurrency() != null
            ? normalizeText(request.sourceCurrency()).toUpperCase(Locale.ROOT)
            : normalizedCurrency;
        BigDecimal exchangeRate = request.exchangeRate() != null ? request.exchangeRate() : BigDecimal.ONE;
        BigDecimal convertedAmount = request.convertedAmount() != null
            ? request.convertedAmount()
            : originalAmount.multiply(exchangeRate);
        booking.setOriginalAmount(originalAmount);
        booking.setSourceCurrency(sourceCurrency);
        booking.setExchangeRate(exchangeRate);
        booking.setConvertedAmount(convertedAmount);
        booking.setDepartureDate(request.departureDate());
        booking.setReturnDate(request.returnDate());
        booking.setPaymentDate(request.paymentDate());
        booking.setStatus(status);

        return toResponse(bookingRepository.save(booking));
    }

    @Override
    public void delete(UUID agencyId, UUID id) {
        validateAgencyId(agencyId);
        bookingRepository.delete(getBookingOrThrow(agencyId, id));
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public BookingResponse findById(UUID agencyId, UUID id) {
        validateAgencyId(agencyId);
        return toResponse(getBookingOrThrow(agencyId, id));
    }

    private Booking getBookingOrThrow(UUID agencyId, UUID id) {
        return bookingRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    private Agency getAgencyOrThrow(UUID agencyId) {
        return agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found with id: " + agencyId));
    }

    private Customer getCustomerOrThrow(UUID agencyId, UUID customerId) {
        return customerRepository.findByIdAndAgencyId(customerId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
    }

    private Supplier getSupplierOrNull(UUID agencyId, UUID supplierId) {
        if (supplierId == null) {
            return null;
        }

        return supplierRepository.findByIdAndAgencyId(supplierId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
    }

    private User getUserOrThrow(UUID userId, UUID agencyId) {
        return userRepository.findById(userId)
                .filter(user -> user.getAgency().getId().equals(agencyId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private void validateReferenceUniqueness(UUID agencyId, String reference) {
        if (bookingRepository.existsByAgencyIdAndReferenceIgnoreCase(agencyId, reference)) {
            throw new ConflictException("Booking reference " + reference + " is already in use");
        }
    }

    private void validateReferenceUniquenessForUpdate(UUID agencyId, String reference, UUID bookingId) {
        if (bookingRepository.existsByAgencyIdAndReferenceIgnoreCaseAndIdNot(agencyId, reference, bookingId)) {
            throw new ConflictException("Booking reference " + reference + " is already in use");
        }
    }

    private BookingStatus parseBookingStatus(String status) {
        try {
            return BookingStatus.valueOf(normalizeEnumValue(status));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid booking status: " + status);
        }
    }

    private void validateDateRange(java.time.LocalDate departureDate, java.time.LocalDate returnDate) {
        if (departureDate != null && returnDate != null && returnDate.isBefore(departureDate)) {
            throw new InvalidRequestException("Return date cannot be before departure date");
        }
    }

    private String normalizeReference(String value) {
        return value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeEnumValue(String value) {
        return value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private void validateAgencyId(UUID agencyId) {
        if (agencyId == null) {
            throw new InvalidRequestException("Agency id is required");
        }
    }

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new InvalidRequestException("User id is required");
        }
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getCustomer().getId(),
                booking.getCustomer().getFullName(),
                booking.getSupplier() != null ? booking.getSupplier().getId() : null,
                booking.getSupplier() != null ? booking.getSupplier().getName() : null,
                booking.getReference(),
                booking.getDescription(),
                booking.getAmount(),
                booking.getCurrency(),
                booking.getOriginalAmount(),
                booking.getSourceCurrency(),
                booking.getExchangeRate(),
                booking.getConvertedAmount(),
                booking.getDepartureDate(),
                booking.getReturnDate(),
                booking.getPaymentDate(),
                booking.getStatus().name()
        );
    }
}