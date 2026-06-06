package org.store.audit.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.audit.application.dto.AuditLogFilter;
import org.store.audit.application.dto.AuditLogResponse;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.model.AuditLog;
import org.store.audit.domain.repository.AuditLogRepository;
import org.store.common.service.GlobalService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuditLogDomainService extends GlobalService<AuditLog, AuditLogRepository> {

    public AuditLogDomainService(AuditLogRepository repository) {
        super(repository);
    }

    public Page<AuditLogResponse> findByFilter(AuditLogFilter filter) {
        return repository.findResponsesByFilter(filter, filter.toPageable());
    }

    /** Returns the most recent LOGIN entry for the given accountId, used to compute session duration at logout. */
    public Optional<AuditLog> findLastLogin(String accountId) {
        return repository.findLastByActionAndAccount(AuditAction.LOGIN, accountId, Pageable.ofSize(1))
                .getContent().stream().findFirst();
    }

    /** Formats the duration between a past timestamp and now as "Xh Ym" or "Ym Zs". */
    public static String formatDuration(LocalDateTime since) {
        Duration d = Duration.between(since, LocalDateTime.now());
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
