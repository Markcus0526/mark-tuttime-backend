package com.tuttime.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Token Reserve Entity
 *
 * <p>Represents token reserves for each blockchain network, including
 * Treasury and HotWallet balances, token price, total supply, and NAV.
 */
@Entity
@Table(name = "token_reserves", schema = "admin_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TokenReserve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reserve_id")
    private Long reserveId;

    @Column(name = "network", nullable = false, unique = true, length = 50)
    private String network;

    @Column(name = "treasury_balance", nullable = false, precision = 36, scale = 18)
    @Builder.Default
    private BigDecimal treasuryBalance = BigDecimal.ZERO;

    @Column(name = "hot_wallet_balance", nullable = false, precision = 36, scale = 18)
    @Builder.Default
    private BigDecimal hotWalletBalance = BigDecimal.ZERO;

    @Column(name = "token_price", precision = 36, scale = 18)
    private BigDecimal tokenPrice;

    @Column(name = "total_supply", precision = 36, scale = 18)
    private BigDecimal totalSupply;

    @Column(name = "nav", precision = 36, scale = 18)
    private BigDecimal nav;

    @Column(name = "last_updated_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt;

    /**
     * Get total balance (treasury + hot wallet)
     */
    public BigDecimal getTotalBalance() {
        return treasuryBalance.add(hotWalletBalance);
    }

    /**
     * Pre-persist hook to set lastUpdatedAt
     */
    @PrePersist
    @PreUpdate
    protected void updateLastUpdatedAt() {
        lastUpdatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}

