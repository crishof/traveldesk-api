package com.crishof.traveldeskapi.security.principal;

import com.crishof.traveldeskapi.model.SecurityAccount;
import com.crishof.traveldeskapi.model.User;
import com.crishof.traveldeskapi.repository.SecurityAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityUserDetailsService implements UserDetailsService {

    private final SecurityAccountRepository securityAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);
        if (email.isBlank()) {
            log.debug("Email is blank");
            throw new UsernameNotFoundException("Email cannot be blank");
        }

        SecurityAccount account = securityAccountRepository.findByUserEmailIgnoreCase(email.trim()).orElseThrow(
                () -> new UsernameNotFoundException("User not found with email: " + email));

        User user = account.getUser();
        log.debug("User loaded successfully: {}", user.getEmail());
        return new SecurityUser(user, account);
    }
}
