package com.pulsebrief.admin.repository;

import com.pulsebrief.admin.domain.AdminUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsernameIgnoreCase(String username);
    long countByRoleCodeAndUserStatus(String roleCode, String userStatus);
}
