package com.tuttime.analytics.repository;

import com.tuttime.analytics.entity.SystemEvent;
import com.tuttime.analytics.entity.UserReliabilityIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserReliabilityRepository extends JpaRepository<UserReliabilityIndex, UUID> {

}