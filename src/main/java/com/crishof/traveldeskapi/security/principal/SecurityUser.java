package com.crishof.traveldeskapi.security.principal;

import com.crishof.traveldeskapi.model.Role;
import com.crishof.traveldeskapi.model.security.SecurityAccount;
import com.crishof.traveldeskapi.model.User;
import com.crishof.traveldeskapi.model.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class SecurityUser implements UserDetails {

    private final UUID id;
    private final UUID agencyId;
    private final String email;
    private final String password;
    private final Role role;
    private final UserStatus status;
    private final boolean emailVerified;
    private final boolean enabled;
    private final boolean locked;

    public SecurityUser(User user, SecurityAccount securityAccount) {
        this.id = user.getId();
        this.agencyId = user.getAgency().getId();
        this.email = user.getEmail();
        this.password = securityAccount.getPasswordHash();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.emailVerified = securityAccount.isEmailVerified();
        this.enabled = securityAccount.isEnabled();
        this.locked = securityAccount.isLocked();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked && status != UserStatus.BLOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled && emailVerified && status == UserStatus.ACTIVE;
    }
}
