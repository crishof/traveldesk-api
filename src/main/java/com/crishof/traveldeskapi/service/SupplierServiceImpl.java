package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.SupplierCreateRequest;
import com.crishof.traveldeskapi.dto.SupplierRequest;
import com.crishof.traveldeskapi.dto.SupplierResponse;
import com.crishof.traveldeskapi.exception.ConflictException;
import com.crishof.traveldeskapi.exception.InvalidRequestException;
import com.crishof.traveldeskapi.exception.ResourceNotFoundException;
import com.crishof.traveldeskapi.mapper.SupplierMapper;
import com.crishof.traveldeskapi.model.Supplier;
import com.crishof.traveldeskapi.model.SupplierType;
import com.crishof.traveldeskapi.model.agency.Agency;
import com.crishof.traveldeskapi.repository.AgencyRepository;
import com.crishof.traveldeskapi.repository.BookingRepository;
import com.crishof.traveldeskapi.repository.SupplierRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
public class SupplierServiceImpl implements SupplierService {

    private final SupplierMapper supplierMapper;
    private final SupplierRepository supplierRepository;
    private final AgencyRepository agencyRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<SupplierResponse> getAll(UUID agencyId) {
        validateAgencyId(agencyId);

        return supplierRepository.findAllByAgencyIdOrderByNameAsc(agencyId).stream()
                .map(supplierMapper::toResponse)
                .toList();
    }

    @Override
    public SupplierResponse create(UUID agencyId, SupplierCreateRequest request) {
        validateAgencyId(agencyId);

        Agency agency = getAgencyOrThrow(agencyId);

        String normalizedName = normalizeRequiredText(request.name(), "Name is required");
        String normalizedCurrency = normalizeRequiredText(request.currency(), "Currency is required").toUpperCase(Locale.ROOT);
        String normalizedPhone = normalizeOptionalText(request.phone());
        SupplierType supplierType = parseOptionalSupplierType(request.serviceType());

        String normalizedEmail = normalizeOptionalEmail(request.email());
        if (normalizedEmail == null) {
            normalizedEmail = generatePlaceholderEmail();
        } else {
            validateSupplierEmailUniqueness(agencyId, normalizedEmail);
        }

        Supplier supplier = supplierMapper.toEntity(request);
        supplier.setAgency(agency);
        supplier.setName(normalizedName);
        supplier.setCurrency(normalizedCurrency);
        supplier.setEmail(normalizedEmail);
        supplier.setPhone(normalizedPhone);
        supplier.setType(supplierType);

        Supplier savedSupplier = supplierRepository.save(supplier);
        return supplierMapper.toResponse(savedSupplier);
    }

    @Override
    public SupplierResponse update(UUID agencyId, UUID id, SupplierRequest request) {
        validateAgencyId(agencyId);

        Supplier supplier = getSupplierOrThrow(agencyId, id);

        String normalizedEmail = normalizeEmail(request.email());
        String normalizedName = normalizeText(request.name());
        String normalizedPhone = normalizePhone(request.phone());
        SupplierType supplierType = parseSupplierType(request.serviceType());

        validateSupplierEmailUniquenessForUpdate(agencyId, normalizedEmail, id);

        supplierMapper.updateEntityFromRequest(request, supplier);
        supplier.setName(normalizedName);
        supplier.setEmail(normalizedEmail);
        supplier.setPhone(normalizedPhone);
        supplier.setType(supplierType);

        Supplier updatedSupplier = supplierRepository.save(supplier);
        return supplierMapper.toResponse(updatedSupplier);
    }

    @Override
    public void delete(UUID agencyId, UUID id) {
        validateAgencyId(agencyId);

        Supplier supplier = getSupplierOrThrow(agencyId, id);

        if (bookingRepository.existsBySupplierId(supplier.getId()) ) {
            throw new ConflictException("Supplier cannot be deleted because it has related bookings or sales");
        }

        supplierRepository.delete(supplier);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public SupplierResponse findById(UUID agencyId, UUID id) {
        validateAgencyId(agencyId);
        return supplierMapper.toResponse(getSupplierOrThrow(agencyId, id));
    }

    private Supplier getSupplierOrThrow(UUID agencyId, UUID id) {
        return supplierRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
    }

    private Agency getAgencyOrThrow(UUID agencyId) {
        return agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found with id: " + agencyId));
    }

    private void validateSupplierEmailUniqueness(UUID agencyId, String normalizedEmail) {
        if (supplierRepository.existsByAgencyIdAndEmailIgnoreCase(agencyId, normalizedEmail)) {
            throw new ConflictException("Supplier email " + normalizedEmail + " is already in use");
        }
    }

    private void validateSupplierEmailUniquenessForUpdate(UUID agencyId, String normalizedEmail, UUID supplierId) {
        if (supplierRepository.existsByAgencyIdAndEmailIgnoreCaseAndIdNot(agencyId, normalizedEmail, supplierId)) {
            throw new ConflictException("Supplier email " + normalizedEmail + " is already in use");
        }
    }

    private SupplierType parseSupplierType(String serviceType) {
        try {
            return SupplierType.valueOf(normalizeEnumValue(serviceType));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid supplier service type: " + serviceType);
        }
    }

    private SupplierType parseOptionalSupplierType(String serviceType) {
        if (serviceType == null || serviceType.isBlank()) {
            return SupplierType.OTHER;
        }
        return parseSupplierType(serviceType);
    }

    private String generatePlaceholderEmail() {
        return "no-email+" + UUID.randomUUID() + "@placeholder.local";
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptionalEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return normalizeEmail(email);
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestException(message);
        }
        return normalizeText(value);
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return normalizeText(value);
    }

    private String normalizeText(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizePhone(String phone) {
        return phone.trim().replaceAll("\\s+", " ");
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
}
