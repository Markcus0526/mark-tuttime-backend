package com.tuttime.restaurant.repository;

import java.util.UUID;

import com.tuttime.restaurant.entity.Branch;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {
}
