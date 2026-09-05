package com.insurance.portal.repository;

import com.insurance.portal.entity.Role;
import com.insurance.portal.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
