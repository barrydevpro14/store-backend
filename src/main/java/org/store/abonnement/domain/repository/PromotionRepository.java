package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
            WHERE (:nom IS NULL OR :nom = '' OR LOWER(promotion.nom) LIKE :nomPattern)
              AND (:actif IS NULL OR promotion.actif = :actif)
              AND (:planId IS NULL OR plan.id = :planId)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', promotion.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', promotion.createdAt) <= CAST(:endDate AS date))
            ORDER BY promotion.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(promotion)
            FROM Promotion promotion
            LEFT JOIN promotion.plan plan
            WHERE (:nom IS NULL OR :nom = '' OR LOWER(promotion.nom) LIKE :nomPattern)
              AND (:actif IS NULL OR promotion.actif = :actif)
              AND (:planId IS NULL OR plan.id = :planId)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', promotion.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', promotion.createdAt) <= CAST(:endDate AS date))
            """)
    Page<PromotionResponse> findResponsesByFilter(
            @Param("nom") String nom,
            @Param("nomPattern") String nomPattern,
            @Param("actif") Boolean actif,
            @Param("planId") java.util.UUID planId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

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
