package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.CustomerRequest;
import com.crishof.traveldeskapi.dto.CustomerResponse;
import com.crishof.traveldeskapi.exception.ConflictException;
import com.crishof.traveldeskapi.exception.InvalidRequestException;
import com.crishof.traveldeskapi.exception.ResourceNotFoundException;
import com.crishof.traveldeskapi.model.Customer;
import com.crishof.traveldeskapi.model.agency.Agency;
import com.crishof.traveldeskapi.repository.AgencyRepository;
import com.crishof.traveldeskapi.repository.BookingRepository;
import com.crishof.traveldeskapi.repository.CustomerRepository;
import com.crishof.traveldeskapi.repository.SaleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final AgencyRepository agencyRepository;
    private final BookingRepository bookingRepository;
    private final SaleRepository saleRepository;

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<CustomerResponse> getAll(UUID agencyId) {
        validateAgencyId(agencyId);

        return customerRepository.findAllByAgencyIdOrderByFullNameAsc(agencyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CustomerResponse create(UUID agencyId, CustomerRequest request) {
        validateAgencyId(agencyId);

        Agency agency = getAgencyOrThrow(agencyId);

        String normalizedFullName = normalizeText(request.fullName());
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedPhone = normalizePhone(request.phone());

        validateCustomerEmailUniqueness(agencyId, normalizedEmail);

        Customer customer = new Customer();
        customer.setAgency(agency);
        customer.setFullName(normalizedFullName);
        customer.setEmail(normalizedEmail);
        customer.setPhone(normalizedPhone);

        Customer savedCustomer = customerRepository.save(customer);
        return toResponse(savedCustomer);
    }

    @Override
    public CustomerResponse update(UUID agencyId, UUID id, CustomerRequest request) {
        validateAgencyId(agencyId);

        Customer customer = getCustomerOrThrow(agencyId, id);

        String normalizedFullName = normalizeText(request.fullName());
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedPhone = normalizePhone(request.phone());

        validateCustomerEmailUniquenessForUpdate(agencyId, normalizedEmail, id);

        customer.setFullName(normalizedFullName);
        customer.setEmail(normalizedEmail);
        customer.setPhone(normalizedPhone);

        Customer updatedCustomer = customerRepository.save(customer);
        return toResponse(updatedCustomer);
    }

    @Override
    public void delete(UUID agencyId, UUID id) {
        validateAgencyId(agencyId);

        Customer customer = getCustomerOrThrow(agencyId, id);

        if (bookingRepository.existsByCustomerId(customer.getId()) || saleRepository.existsByCustomerId(customer.getId())) {
            throw new ConflictException("Customer cannot be deleted because it has related bookings or sales");
        }

        customerRepository.delete(customer);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public CustomerResponse findById(UUID agencyId, UUID id) {
        validateAgencyId(agencyId);
        return toResponse(getCustomerOrThrow(agencyId, id));
    }

    private Customer getCustomerOrThrow(UUID agencyId, UUID id) {
        return customerRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    private Agency getAgencyOrThrow(UUID agencyId) {
        return agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found with id: " + agencyId));
    }

    private void validateCustomerEmailUniqueness(UUID agencyId, String normalizedEmail) {
        if (customerRepository.existsByAgencyIdAndEmailIgnoreCase(agencyId, normalizedEmail)) {
            throw new ConflictException("Customer email " + normalizedEmail + " is already in use");
        }
    }

    private void validateCustomerEmailUniquenessForUpdate(UUID agencyId, String normalizedEmail, UUID customerId) {
        if (customerRepository.existsByAgencyIdAndEmailIgnoreCaseAndIdNot(agencyId, normalizedEmail, customerId)) {
            throw new ConflictException("Customer email " + normalizedEmail + " is already in use");
        }
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizePhone(String phone) {
        return phone.trim().replaceAll("\\s+", " ");
    }

    private void validateAgencyId(UUID agencyId) {
        if (agencyId == null) {
            throw new InvalidRequestException("Agency id is required");
        }
    }
}
