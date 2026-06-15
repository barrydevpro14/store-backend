package org.store.audit.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import java.time.LocalDate;
import java.util.UUID;

public record AuditLogFilter(
        AuditAction action,
        AuditEntityType entityType,
        UUID entrepriseId,
        UUID magasinId,
        String performedByLabel,
        LocalDate createdStartDate,
        LocalDate createdEndDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
