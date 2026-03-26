package com.crishof.traveldeskapi.repository;

import com.crishof.traveldeskapi.model.security.EmailVerificationToken;
import com.crishof.traveldeskapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findTopByUserAndCodeAndUsedFalseOrderByCreatedAtDesc(User user, String code);

    @Modifying
    @Transactional
    void deleteByUser(User user);
}
