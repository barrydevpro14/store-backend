package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.PromotionFilter;
import org.store.abonnement.application.dto.PromotionResponse;
import org.store.abonnement.domain.model.Promotion;
import org.store.common.repository.BaseRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends BaseRepository<Promotion> {

    @Query(value = """
            SELECT new org.store.abonnement.application.dto.PromotionResponse(promotion)
            FROM Promotion promotion
            LEFT JOIN promotion.plan plan
            WHERE (:#{#filter.nom}    IS NULL OR LOWER(promotion.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%')))
              AND (:#{#filter.actif}  IS NULL OR promotion.actif = :#{#filter.actif})
              AND (:#{#filter.planId} IS NULL OR plan.id         = :#{#filter.planId})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR promotion.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR promotion.createdAt <  :#{#filter.createdEndDateTime()})
            ORDER BY promotion.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(promotion)
            FROM Promotion promotion
            LEFT JOIN promotion.plan plan
            WHERE (:#{#filter.nom}    IS NULL OR LOWER(promotion.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%')))
              AND (:#{#filter.actif}  IS NULL OR promotion.actif = :#{#filter.actif})
              AND (:#{#filter.planId} IS NULL OR plan.id         = :#{#filter.planId})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR promotion.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR promotion.createdAt <  :#{#filter.createdEndDateTime()})
            """)
    Page<PromotionResponse> findResponsesByFilter(@Param("filter") PromotionFilter filter, Pageable pageable);

    @Query("""
            SELECT new org.store.abonnement.application.dto.PromotionResponse(promotion)
            FROM Promotion promotion
            WHERE promotion.actif    = true
              AND promotion.dateDebut <= :today
              AND promotion.dateFin   >= :today
              AND promotion.plan      IS NULL
            ORDER BY promotion.dateDebut ASC
            """)
    List<PromotionResponse> findActiveGlobalResponses(@Param("today") LocalDate today);

    @Query("""
            SELECT new org.store.abonnement.application.dto.PromotionResponse(promotion)
            FROM Promotion promotion
            LEFT JOIN promotion.plan plan
            WHERE promotion.actif    = true
              AND promotion.dateDebut <= :today
              AND promotion.dateFin   >= :today
              AND promotion.plan      IS NOT NULL
            ORDER BY promotion.dateDebut ASC
            """)
    List<PromotionResponse> findActiveScopedResponses(@Param("today") LocalDate today);

    @Query("""
            SELECT promotion
            FROM Promotion promotion
            WHERE promotion.actif    = true
              AND promotion.dateDebut <= :today
              AND promotion.dateFin   >= :today
              AND promotion.plan.id   = :planId
            ORDER BY promotion.valeurReduction DESC
            """)
    Optional<Promotion> findFirstActivePromotionForPlan(@Param("planId") UUID planId,
                                                       @Param("today") LocalDate today);
}
