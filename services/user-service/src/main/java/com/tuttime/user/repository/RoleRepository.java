package com.tuttime.user.repository;

import com.tuttime.user.entity.Role;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<UserRole> findByName(String name);
}

