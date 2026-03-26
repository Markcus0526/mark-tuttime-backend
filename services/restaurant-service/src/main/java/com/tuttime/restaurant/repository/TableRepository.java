package com.tuttime.restaurant.repository;

import java.util.UUID;

import com.tuttime.restaurant.entity.Table;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableRepository extends JpaRepository<Table, UUID> {
}
