package org.store.audit.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.audit.application.dto.AuditLogFilter;
import org.store.audit.application.dto.AuditLogResponse;
import org.store.audit.application.service.IAuditLogService;
import org.store.audit.domain.service.AuditLogDomainService;
import org.store.common.service.ValidatorService;

/**
 * Exposes the paginated audit log to the presentation layer.
 * Write side goes through AuditEventListener (async, fire-and-forget).
 */
@Service
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements IAuditLogService {

    private final AuditLogDomainService auditLogDomainService;
    private final ValidatorService validatorService;

    public AuditLogServiceImpl(AuditLogDomainService auditLogDomainService,
                                ValidatorService validatorService) {
        this.auditLogDomainService = auditLogDomainService;
        this.validatorService = validatorService;
    }

    @Override
    public Page<AuditLogResponse> findAll(AuditLogFilter filter) {
        validatorService.validate(filter);
        return auditLogDomainService.findByFilter(filter);
    }
}
