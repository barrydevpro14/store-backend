package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.PlanAbonnementResponse;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.common.repository.BaseRepository;

import java.util.List;
import java.util.Optional;

public interface PlanAbonnementRepository extends BaseRepository<PlanAbonnement> {

    Optional<PlanAbonnement> findFirstByActifTrue();

    boolean existsByNom(String nom);

    @Query(value = """
            SELECT new org.store.abonnement.application.dto.PlanAbonnementResponse(plan)
            FROM PlanAbonnement plan
            WHERE (:nom IS NULL OR :nom = '' OR LOWER(plan.nom) LIKE :nomPattern)
              AND (:actif IS NULL OR plan.actif = :actif)
              AND (:visible IS NULL OR plan.visible = :visible)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', plan.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', plan.createdAt) <= CAST(:endDate AS date))
            ORDER BY plan.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(plan)
            FROM PlanAbonnement plan
            WHERE (:nom IS NULL OR :nom = '' OR LOWER(plan.nom) LIKE :nomPattern)
              AND (:actif IS NULL OR plan.actif = :actif)
              AND (:visible IS NULL OR plan.visible = :visible)
              AND (:startDate IS NULL OR :startDate = '' OR FUNCTION('DATE', plan.createdAt) >= CAST(:startDate AS date))
              AND (:endDate   IS NULL OR :endDate   = '' OR FUNCTION('DATE', plan.createdAt) <= CAST(:endDate AS date))
            """)
    Page<PlanAbonnementResponse> findResponsesByFilter(
            @Param("nom") String nom,
            @Param("nomPattern") String nomPattern,
            @Param("actif") Boolean actif,
            @Param("visible") Boolean visible,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT p FROM PlanAbonnement p
            LEFT JOIN FETCH p.tarifs
            WHERE p.actif = true AND p.visible = true
            ORDER BY p.ordre ASC, p.nom ASC
            """)
    List<PlanAbonnement> findPublicPlans();

    /**
     * Plans the OWNER can subscribe to: active, visible, and not a trial plan.
     */
    @Query("""
            SELECT DISTINCT p FROM PlanAbonnement p
            LEFT JOIN FETCH p.tarifs
            WHERE p.actif = true AND p.visible = true AND p.trial = false
            ORDER BY p.ordre ASC, p.nom ASC
            """)
    List<PlanAbonnement> findSubscribablePlans();

    /** Returns the first active trial plan (ordered by createdAt ASC), or empty if none exists. */
    @Query("SELECT p FROM PlanAbonnement p WHERE p.trial = true AND p.actif = true ORDER BY p.createdAt ASC")
    Optional<PlanAbonnement> findFirstTrialPlan();
}
