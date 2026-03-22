package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.DashboardStatsResponse;
import com.crishof.traveldeskapi.exception.InvalidRequestException;
import com.crishof.traveldeskapi.exception.ResourceNotFoundException;
import com.crishof.traveldeskapi.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(Transactional.TxType.SUPPORTS)
public class DashboardServiceImpl implements DashboardService {

    private final AgencyRepository agencyRepository;
    private final SaleRepository saleRepository;
    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;

    @Override
    public DashboardStatsResponse getStats(UUID agencyId) {
        validateAgencyId(agencyId);
        validateAgencyExists(agencyId);

        long totalSales = saleRepository.countByAgencyId(agencyId);
        long totalBookings = bookingRepository.countByAgencyId(agencyId);
        long totalCustomers = customerRepository.countByAgencyId(agencyId);
        long totalSuppliers = supplierRepository.countByAgencyId(agencyId);

        return new DashboardStatsResponse(
                safeLongToInt(totalSales),
                safeLongToInt(totalBookings),
                safeLongToInt(totalCustomers),
                safeLongToInt(totalSuppliers)
        );
    }

    private void validateAgencyExists(UUID agencyId) {
        if (!agencyRepository.existsById(agencyId)) {
            throw new ResourceNotFoundException("Agency not found with id: " + agencyId);
        }
    }

    private int safeLongToInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }

    private void validateAgencyId(UUID agencyId) {
        if (agencyId == null) {
            throw new InvalidRequestException("Agency id is required");
        }
    }
}
