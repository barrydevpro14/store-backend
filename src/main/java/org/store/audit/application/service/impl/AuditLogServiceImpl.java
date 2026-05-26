package org.store.audit.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.audit.application.dto.AuditLogFilter;
import org.store.audit.application.dto.AuditLogResponse;
import org.store.audit.application.service.IAuditLogService;
import org.store.audit.domain.service.AuditLogDomainService;
import org.store.common.service.ValidatorService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

/**
 * Exposes the paginated audit log to the presentation layer.
 * Non-ADMIN callers are automatically scoped to their own entreprise —
 * the entrepriseId request param is ignored for them (security enforcement).
 * Write side goes through AuditEventListener (async, fire-and-forget).
 */
@Service
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements IAuditLogService {

    private final AuditLogDomainService auditLogDomainService;
    private final ValidatorService validatorService;
    private final ICurrentUserService currentUserService;

    public AuditLogServiceImpl(AuditLogDomainService auditLogDomainService,
                                ValidatorService validatorService,
                                ICurrentUserService currentUserService) {
        this.auditLogDomainService = auditLogDomainService;
        this.validatorService = validatorService;
        this.currentUserService = currentUserService;
    }

    @Override
    public Page<AuditLogResponse> findAll(AuditLogFilter filter) {
        validatorService.validate(filter);
        return auditLogDomainService.findByFilter(scopeFilter(filter));
    }

    private AuditLogFilter scopeFilter(AuditLogFilter filter) {
        UserPrincipal caller = currentUserService.getCurrent();
        if (caller.entrepriseId() == null) return filter; // ADMIN — full access
        if (caller.magasinId() != null) {
            // MANAGER — scoped to their magasin
            return new AuditLogFilter(filter.action(), filter.entityType(),
                    caller.entrepriseId(), caller.magasinId(),
                    filter.performedByLabel(), filter.createdStartDate(), filter.createdEndDate(),
                    filter.page(), filter.size());
        }
        // OWNER — scoped to their entreprise
        return new AuditLogFilter(filter.action(), filter.entityType(),
                caller.entrepriseId(), null,
                filter.performedByLabel(), filter.createdStartDate(), filter.createdEndDate(),
                filter.page(), filter.size());
    }
}
