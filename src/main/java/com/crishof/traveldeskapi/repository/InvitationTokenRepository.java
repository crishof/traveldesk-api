package com.crishof.traveldeskapi.repository;

import com.crishof.traveldeskapi.model.InvitationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvitationTokenRepository extends JpaRepository<InvitationToken, UUID> {

    Optional<InvitationToken> findByToken(String token);

    Optional<InvitationToken> findTopByEmailIgnoreCaseAndUsedFalseOrderByCreatedAtDesc(String email);
}
