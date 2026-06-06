package org.store.audit.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.audit.domain.model.AuditLog;
import org.store.audit.domain.repository.AuditLogRepository;

import java.util.UUID;

public interface AuditLogJpaRepository extends JpaRepository<AuditLog, UUID>, AuditLogRepository {}
