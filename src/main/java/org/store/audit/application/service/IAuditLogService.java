package org.store.audit.application.service;

import org.springframework.data.domain.Page;
import org.store.audit.application.dto.AuditLogFilter;
import org.store.audit.application.dto.AuditLogResponse;

public interface IAuditLogService {
    /** Paginated filtered audit log listing. */
    Page<AuditLogResponse> findAll(AuditLogFilter filter);
}
