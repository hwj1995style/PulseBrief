package com.pulsebrief.admin.repository;

import com.pulsebrief.admin.domain.AdminOperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminOperationLogRepository extends JpaRepository<AdminOperationLog, Long> {
    Page<AdminOperationLog> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    Page<AdminOperationLog> findByOperationModuleOrderByCreatedAtDescIdDesc(String operationModule, Pageable pageable);
}
