package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.*;
import com.crishof.traveldeskapi.exception.InvalidRequestException;
import com.crishof.traveldeskapi.exception.ResourceNotFoundException;
import com.crishof.traveldeskapi.model.*;
import com.crishof.traveldeskapi.model.agency.Agency;
import com.crishof.traveldeskapi.repository.*;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesServiceImpl implements SalesService {

    private final SaleRepository saleRepository;
    private final AgencyRepository agencyRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<SaleResponse> getAll(UUID agencyId) {
        validateAgencyId(agencyId);

        return saleRepository.findAllByAgencyIdOrderBySaleDateDesc(agencyId).stream().map(this::toResponse).toList();
    }

    @Override
    public SaleResponse create(UUID agencyId, UUID userId, SaleRequest request) {
        validateAgencyId(agencyId);
        validateUserId(userId);

        Agency agency = getAgencyOrThrow(agencyId);

        Customer customer = new Customer();
        if (request.customerId() != null) {

            customer = getCustomerOrThrow(agencyId, request.customerId());
        } else {
            customer.setAgency(agency);
            customer.setFullName(request.customerName());
            customerRepository.save(customer);

        }

        User createdBy = getUserOrThrow(userId, agencyId);

        Sale sale = new Sale();
        sale.setAgency(agency);
        sale.setCustomer(customer);

        sale.setCreatedBy(createdBy);
        sale.setDestination(normalizeText(request.destination()));
        sale.setAmount(request.amount());
        sale.setCurrency(request.currency().toUpperCase(Locale.ROOT));
        sale.setCommissionPercentage(createdBy.getCommissionPercentage());
        sale.setStatus(parseSaleStatus(request.status()));
        sale.setDepartureDate(request.departureDate());

        if (request.description() != null && !request.description().isBlank()) {
            sale.setDescription(normalizeText(request.description()));
        }

        return toResponse(saleRepository.save(sale));
    }

    @Override
    public SaleResponse update(UUID agencyId, UUID id, SaleUpdateRequest request) {
        validateAgencyId(agencyId);

        Sale sale = getSaleOrThrow(agencyId, id);

        if (request.customerId() != null) {
            Customer customer = getCustomerOrThrow(agencyId, request.customerId());
            sale.setCustomer(customer);
        }

        if (request.destination() != null) {
            sale.setDestination(normalizeText(request.destination()));
        }

        if (request.amount() != null) {
            sale.setAmount(request.amount());
        }

        if (request.currency() != null) {
            sale.setCurrency(normalizeText(request.currency()).toUpperCase(Locale.ROOT));
        }

        if (request.status() != null) {
            sale.setStatus(parseSaleStatus(request.status()));
        }

        if (request.departureDate() != null) {
            sale.setDepartureDate(request.departureDate());
        }

        if (request.description() != null) {
            sale.setDescription(normalizeText(request.description()));
        }

        return toResponse(saleRepository.save(sale));
    }

    @Override
    public void delete(UUID agencyId, UUID id) {
        validateAgencyId(agencyId);
        saleRepository.delete(getSaleOrThrow(agencyId, id));
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public SaleResponse findById(UUID agencyId, UUID id) {
        validateAgencyId(agencyId);
        return toResponse(getSaleOrThrow(agencyId, id));
    }

    private Sale getSaleOrThrow(UUID agencyId, UUID id) {
        return saleRepository.findByIdAndAgencyId(id, agencyId).orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + id));
    }

    private Agency getAgencyOrThrow(UUID agencyId) {
        return agencyRepository.findById(agencyId).orElseThrow(() -> new ResourceNotFoundException("Agency not found with id: " + agencyId));
    }

    private Customer getCustomerOrThrow(UUID agencyId, UUID customerId) {
        return customerRepository.findByIdAndAgencyId(customerId, agencyId).orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
    }

    private Supplier getSupplierOrNull(UUID agencyId, UUID supplierId) {
        if (supplierId == null) {
            return null;
        }

        return supplierRepository.findByIdAndAgencyId(supplierId, agencyId).orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));
    }

    private User getUserOrThrow(UUID userId, UUID agencyId) {
        return userRepository.findById(userId).filter(user -> user.getAgency().getId().equals(agencyId)).orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private SaleStatus parseSaleStatus(String status) {
        try {
            return SaleStatus.valueOf(normalizeEnumValue(status));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid sale status: " + status);
        }
    }

    private String normalizeText(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeEnumValue(String value) {
        return value.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ROOT);
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

    private SaleResponse toResponse(Sale sale) {
        return new SaleResponse(sale.getId(), sale.getCustomer().getId(), sale.getCustomer().getFullName(), sale.getCreatedBy().getId(), sale.getDestination(), sale.getAmount(), sale.getCurrency(), sale.getStatus().name(), sale.getPaidAmount(), sale.getDepartureDate(), sale.getSaleDate(), sale.getCommissionPercentage());
    }

    @Override
    public SaleResponse registerPayment(UUID agencyId, UUID saleId, @Valid PaymentRequest request) {
        validateAgencyId(agencyId);

        Sale sale = getSaleOrThrow(agencyId, saleId);

        if (!sale.getCustomer().getId().equals(request.customerId())) {
            throw new InvalidRequestException("Customer ID does not match the sale's customer");
        }

        Payment payment = new Payment();
        payment.setSale(sale);
        payment.setOriginalAmount(request.originalAmount());
        payment.setSourceCurrency(request.sourceCurrency().toUpperCase(Locale.ROOT));
        payment.setDescription(normalizeText(request.description()));
        payment.setExchangeRate(request.exchangeRate());
        payment.setConvertedAmount(request.convertedAmount());

        sale.getPayments().add(payment);
        sale.setPaidAmount(sale.getPaidAmount().add(payment.getConvertedAmount()));

        if (sale.getPaidAmount().compareTo(sale.getAmount()) >= 0 && sale.getStatus() == SaleStatus.CREATED) {
            sale.setStatus(SaleStatus.CONFIRMED);
        }

        saleRepository.save(sale);

        return toResponse(sale);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<PaymentResponse> getPaymentsForSale(UUID agencyId, UUID saleId) {
        validateAgencyId(agencyId);

        Sale sale = saleRepository.findByIdAndAgencyIdWithPayments(saleId, agencyId).orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + saleId));

        return sale.getPayments().stream().sorted((p1, p2) -> p2.getPaymentDate().compareTo(p1.getPaymentDate())).map(this::toPaymentResponse).toList();
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOriginalAmount(), payment.getSourceCurrency(), payment.getDescription(), payment.getExchangeRate(), payment.getConvertedAmount(), payment.getPaymentDate());
    }

    @Override
    public SaleResponse deletePayment(UUID agencyId, UUID saleId, UUID paymentId) {
        validateAgencyId(agencyId);

        Sale sale = saleRepository.findByIdAndAgencyIdWithPayments(saleId, agencyId).orElseThrow(() -> new ResourceNotFoundException("Sale not found with id: " + saleId));

        Payment paymentToDelete = sale.getPayments().stream().filter(p -> p.getId().equals(paymentId)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId + " for sale: " + saleId));

        sale.getPayments().remove(paymentToDelete);
        sale.setPaidAmount(sale.getPaidAmount().subtract(paymentToDelete.getConvertedAmount()));

        if (sale.getPaidAmount().compareTo(sale.getAmount()) < 0 && sale.getStatus() == SaleStatus.CONFIRMED) {
            sale.setStatus(SaleStatus.CREATED);
        }

        saleRepository.save(sale);

        return toResponse(sale);
    }
}