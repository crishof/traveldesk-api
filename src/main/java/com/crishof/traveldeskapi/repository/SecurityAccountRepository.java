package com.crishof.traveldeskapi.repository;

import com.crishof.traveldeskapi.model.SecurityAccount;
import com.crishof.traveldeskapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SecurityAccountRepository extends JpaRepository<SecurityAccount, UUID> {

    Optional<SecurityAccount> findByUser(User user);

    Optional<SecurityAccount> findByUserEmailIgnoreCase(String email);
}
