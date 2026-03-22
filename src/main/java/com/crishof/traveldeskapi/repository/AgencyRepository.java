package com.crishof.traveldeskapi.repository;

import com.crishof.traveldeskapi.model.agency.Agency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgencyRepository extends JpaRepository<Agency, UUID> {

    Optional<Agency> findByNormalizedName(String normalizedName);
}