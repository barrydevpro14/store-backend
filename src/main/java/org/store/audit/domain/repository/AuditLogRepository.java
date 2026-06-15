package org.store.audit.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.audit.application.dto.AuditLogResponse;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.audit.domain.model.AuditLog;
import org.store.common.repository.BaseRepository;

import java.util.UUID;

public interface AuditLogRepository extends BaseRepository<AuditLog> {

    @Query(value = """
            SELECT new org.store.audit.application.dto.AuditLogResponse(log)
            FROM AuditLog log
            WHERE (:action IS NULL OR log.action = :action)
              AND (:entityType IS NULL OR log.entityType = :entityType)
              AND (:entrepriseId IS NULL OR log.entrepriseId = :entrepriseId)
              AND (:magasinId IS NULL OR log.magasinId = :magasinId)
              AND (:performedByLabel IS NULL OR :performedByLabel = '' OR LOWER(log.performedByLabel) LIKE :performedByLabelPattern)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', log.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', log.createdAt) <= CAST(:endDate AS date))
            ORDER BY log.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(log) FROM AuditLog log
            WHERE (:action IS NULL OR log.action = :action)
              AND (:entityType IS NULL OR log.entityType = :entityType)
              AND (:entrepriseId IS NULL OR log.entrepriseId = :entrepriseId)
              AND (:magasinId IS NULL OR log.magasinId = :magasinId)
              AND (:performedByLabel IS NULL OR :performedByLabel = '' OR LOWER(log.performedByLabel) LIKE :performedByLabelPattern)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', log.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', log.createdAt) <= CAST(:endDate AS date))
            """)
    Page<AuditLogResponse> findResponsesByFilter(
            @Param("action") AuditAction action,
            @Param("entityType") AuditEntityType entityType,
            @Param("entrepriseId") UUID entrepriseId,
            @Param("magasinId") UUID magasinId,
            @Param("performedByLabel") String performedByLabel,
            @Param("performedByLabelPattern") String performedByLabelPattern,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

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
