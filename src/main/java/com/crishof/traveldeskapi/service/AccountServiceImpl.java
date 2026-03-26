package com.crishof.traveldeskapi.service;

import com.crishof.traveldeskapi.dto.*;
import com.crishof.traveldeskapi.exception.ConflictException;
import com.crishof.traveldeskapi.exception.InvalidRequestException;
import com.crishof.traveldeskapi.exception.ResourceNotFoundException;
import com.crishof.traveldeskapi.model.agency.Agency;
import com.crishof.traveldeskapi.model.agency.CommissionType;
import com.crishof.traveldeskapi.model.User;
import com.crishof.traveldeskapi.repository.AgencyRepository;
import com.crishof.traveldeskapi.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final AgencyRepository agencyRepository;

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public AccountResponse getAccount(UUID userId) {
        validateUserId(userId);
        return toAccountResponse(getUserOrThrow(userId));
    }

    @Override
    public AccountResponse updateAccount(UUID userId, AccountRequest request) {
        validateUserId(userId);

        User user = getUserOrThrow(userId);

        String normalizedFullName = normalizeText(request.fullName());
        String normalizedEmail = normalizeEmail(request.email());

        validateUserEmailUniquenessForUpdate(normalizedEmail, userId);

        user.setFullName(normalizedFullName);
        user.setEmail(normalizedEmail);

        return toAccountResponse(userRepository.save(user));
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public AgencySettingsResponse getAgencySettings(UUID agencyId) {
        validateAgencyId(agencyId);
        return toAgencySettingsResponse(getAgencyOrThrow(agencyId));
    }

    @Override
    public AgencySettingsResponse updateAgencySettings(UUID agencyId, AgencySettingsRequest request) {
        validateAgencyId(agencyId);

        Agency agency = getAgencyOrThrow(agencyId);

        String normalizedAgencyName = normalizeText(request.agencyName());
        String normalizedAgencySlug = normalizeAgencyName(normalizedAgencyName);
        String normalizedCurrency = normalizeCurrency(request.currency());
        String normalizedTimeZone = normalizeTimeZone(request.timeZone());

        validateAgencyNameUniquenessForUpdate(normalizedAgencySlug, agencyId);

        agency.setName(normalizedAgencyName);
        agency.setNormalizedName(normalizedAgencySlug);
        agency.setCurrency(normalizedCurrency);
        agency.setTimeZone(normalizedTimeZone);

        return toAgencySettingsResponse(agencyRepository.save(agency));
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public CommissionSettingsResponse getCommissionSettings(UUID agencyId) {
        validateAgencyId(agencyId);
        return toCommissionSettingsResponse(getAgencyOrThrow(agencyId));
    }

    @Override
    public CommissionSettingsResponse updateCommissionSettings(UUID agencyId, CommissionSettingsRequest request) {
        validateAgencyId(agencyId);

        Agency agency = getAgencyOrThrow(agencyId);
        CommissionType commissionType = parseCommissionType(request.commissionType());

        validateCommissionValue(commissionType, request.commissionValue());

        agency.setCommissionType(commissionType);
        agency.setCommissionValue(request.commissionValue());

        return toCommissionSettingsResponse(agencyRepository.save(agency));
    }

    private User getUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private Agency getAgencyOrThrow(UUID agencyId) {
        return agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found with id: " + agencyId));
    }

    private void validateUserEmailUniquenessForUpdate(String normalizedEmail, UUID userId) {
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, userId)) {
            throw new ConflictException("User email " + normalizedEmail + " is already in use");
        }
    }

    private void validateAgencyNameUniquenessForUpdate(String normalizedAgencySlug, UUID agencyId) {
        agencyRepository.findByNormalizedName(normalizedAgencySlug)
                .filter(existingAgency -> !existingAgency.getId().equals(agencyId))
                .ifPresent(existingAgency -> {
                    throw new ConflictException("Agency name is already in use");
                });
    }

    private CommissionType parseCommissionType(String commissionType) {
        try {
            return CommissionType.valueOf(normalizeEnumValue(commissionType));
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid commission type: " + commissionType);
        }
    }

    private void validateCommissionValue(CommissionType commissionType, java.math.BigDecimal commissionValue) {
        if (commissionType == CommissionType.PERCENTAGE
                && commissionValue.compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
            throw new InvalidRequestException("Commission percentage cannot be greater than 100");
        }
    }

    private AccountResponse toAccountResponse(User user) {
        return new AccountResponse(
                user.getFullName(),
                user.getEmail(),
                user.getStatus().name()
        );
    }

    private AgencySettingsResponse toAgencySettingsResponse(Agency agency) {
        return new AgencySettingsResponse(
                agency.getName(),
                agency.getCurrency(),
                agency.getTimeZone()
        );
    }

    private CommissionSettingsResponse toCommissionSettingsResponse(Agency agency) {
        return new CommissionSettingsResponse(
                agency.getCommissionType().name(),
                agency.getCommissionValue()
        );
    }

    private String normalizeText(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeCurrency(String currency) {
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(normalized);
        } catch (IllegalArgumentException ex) {
            throw new InvalidRequestException("Invalid currency: " + currency);
        }
        return normalized;
    }

    private String normalizeTimeZone(String timeZone) {
        String normalized = timeZone.trim();
        try {
            ZoneId.of(normalized);
        } catch (Exception ex) {
            throw new InvalidRequestException("Invalid time zone: " + timeZone);
        }
        return normalized;
    }

    private String normalizeEnumValue(String value) {
        return value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
    }

    private String normalizeAgencyName(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new InvalidRequestException("User id is required");
        }
    }

    private void validateAgencyId(UUID agencyId) {
        if (agencyId == null) {
            throw new InvalidRequestException("Agency id is required");
        }
    }
}
