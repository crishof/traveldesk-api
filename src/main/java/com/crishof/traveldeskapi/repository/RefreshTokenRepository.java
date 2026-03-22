package com.crishof.traveldeskapi.repository;

import com.crishof.traveldeskapi.model.RefreshToken;
import com.crishof.traveldeskapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    List<RefreshToken> findAllByUserAndRevokedFalse(User user);
}