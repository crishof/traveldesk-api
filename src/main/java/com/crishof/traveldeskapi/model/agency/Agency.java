package com.crishof.traveldeskapi.model.agency;

import com.crishof.traveldeskapi.model.*;
import com.crishof.traveldeskapi.model.CommissionType;
import com.crishof.traveldeskapi.model.ThemeMode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tbl_agencies")
public class Agency implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String normalizedName;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(nullable = false, length = 60)
    private String timeZone = "UTC";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CommissionType commissionType = CommissionType.PERCENTAGE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal commissionValue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ThemeMode themeMode = ThemeMode.LIGHT;

    @Column(nullable = false, length = 20)
    private String primaryColor = "#FFFFFF";

    @Column(nullable = false, length = 20)
    private String secondaryColor = "#111827";

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = false)
    private Set<User> users = new LinkedHashSet<>();

    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = false)
    private Set<Customer> customers = new LinkedHashSet<>();

    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = false)
    private Set<Supplier> suppliers = new LinkedHashSet<>();

    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = false)
    private Set<Sale> sales = new LinkedHashSet<>();

    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = false)
    private Set<Booking> bookings = new LinkedHashSet<>();

    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, orphanRemoval = false)
    private Set<TeamInvitation> invitations = new LinkedHashSet<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
