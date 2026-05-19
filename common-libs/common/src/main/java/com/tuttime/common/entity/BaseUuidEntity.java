package com.tuttime.common.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseUuidEntity extends BaseTimeStampEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Spring Boot 3.3 / JPA 3.1 feature
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
}