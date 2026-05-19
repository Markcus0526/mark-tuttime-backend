package com.tuttime.user.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import com.tuttime.user.entity.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "user_auth_methods", uniqueConstraints = {
        @UniqueConstraint(name = "unique_provider_id", columnNames = {"auth_type", "provider_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserAuthMethod extends BaseTimeStampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // SERIAL maps to IDENTITY
    private Integer id; // Change from UUID to Integer

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class) // Maps Java Enum to PG ENUM type
    @Column(name = "auth_type", nullable = false, columnDefinition = "auth_provider")
    private AuthProvider authType;

    @Column(name = "provider_id", columnDefinition = "TEXT")
    private String providerId;

    @Column(name = "credential", columnDefinition = "TEXT")
    private String credential;

    @Column(name = "verified", nullable = false)
    @Builder.Default
    private boolean verified = false;
}

