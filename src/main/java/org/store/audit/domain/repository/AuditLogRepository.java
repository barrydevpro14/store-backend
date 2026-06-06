package org.store.audit.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.audit.application.dto.AuditLogFilter;
import org.store.audit.application.dto.AuditLogResponse;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.model.AuditLog;
import org.store.common.repository.BaseRepository;

public interface AuditLogRepository extends BaseRepository<AuditLog> {

    @Query(value = """
            SELECT new org.store.audit.application.dto.AuditLogResponse(log)
            FROM AuditLog log
            WHERE (:#{#filter.action} IS NULL OR log.action = :#{#filter.action})
              AND (:#{#filter.entityType} IS NULL OR log.entityType = :#{#filter.entityType})
              AND (:#{#filter.entrepriseId} IS NULL OR log.entrepriseId = :#{#filter.entrepriseId})
              AND (:#{#filter.magasinId} IS NULL OR log.magasinId = :#{#filter.magasinId})
              AND (:#{#filter.performedByLabel} IS NULL
                   OR LOWER(log.performedByLabel) LIKE LOWER(CONCAT('%', :#{#filter.performedByLabel}, '%')))
              AND log.createdAt >= :#{#filter.createdStartDateTime()}
              AND log.createdAt <  :#{#filter.createdEndDateTime()}
            ORDER BY log.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(log) FROM AuditLog log
            WHERE (:#{#filter.action} IS NULL OR log.action = :#{#filter.action})
              AND (:#{#filter.entityType} IS NULL OR log.entityType = :#{#filter.entityType})
              AND (:#{#filter.entrepriseId} IS NULL OR log.entrepriseId = :#{#filter.entrepriseId})
              AND (:#{#filter.magasinId} IS NULL OR log.magasinId = :#{#filter.magasinId})
              AND (:#{#filter.performedByLabel} IS NULL
                   OR LOWER(log.performedByLabel) LIKE LOWER(CONCAT('%', :#{#filter.performedByLabel}, '%')))
              AND log.createdAt >= :#{#filter.createdStartDateTime()}
              AND log.createdAt <  :#{#filter.createdEndDateTime()}
            """)
    Page<AuditLogResponse> findResponsesByFilter(@Param("filter") AuditLogFilter filter, Pageable pageable);

    @Query("""
            SELECT log FROM AuditLog log
            WHERE log.action = :action
              AND log.performedBy = :accountId
            ORDER BY log.createdAt DESC
            """)
    Page<AuditLog> findLastByActionAndAccount(@Param("action") AuditAction action,
                                              @Param("accountId") String accountId,
                                              Pageable pageable);
}
