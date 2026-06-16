package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.PlanAbonnementResponse;
import org.store.abonnement.application.dto.PublicPlanResponse;
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
            SELECT new org.store.abonnement.application.dto.PublicPlanResponse(
                    plan.id, plan.nom, plan.description, plan.prix,
                    plan.nombreMagasinsMax, plan.nombreEmployesMax,
                    plan.gestionStock, plan.gestionVente, plan.gestionAchat, plan.gestionComptabilite,
                     plan.ordre)
            FROM PlanAbonnement plan
            WHERE plan.actif = true AND plan.visible = true
            ORDER BY plan.ordre ASC, plan.nom ASC
            """)
    List<PublicPlanResponse> findPublicResponses();

    /**
     * Same shape as {@link #findPublicResponses()} but restricted to plans the OWNER can actually
     * subscribe to. A plan is "subscribable" iff it is active + visible AND has at least one active
     * non-trial {@code TypePlanAbonnement}. The trial plan (whose active types are all flagged
     * {@code trial=true}) is therefore filtered out at the DB level — the OWNER already owns a
     * TRIAL Abonnement at signup, so re-subscribing to it is meaningless.
     */
    @Query("""
            SELECT new org.store.abonnement.application.dto.PublicPlanResponse(
                    plan.id, plan.nom, plan.description, plan.prix,
                    plan.nombreMagasinsMax, plan.nombreEmployesMax,
                    plan.gestionStock, plan.gestionVente, plan.gestionAchat, plan.gestionComptabilite,
                     plan.ordre)
            FROM PlanAbonnement plan
            WHERE plan.actif = true AND plan.visible = true
              AND EXISTS (
                SELECT 1
                FROM TypePlanAbonnement type
                WHERE type.plan = plan
                  AND type.actif = true
                  AND type.trial = false
              )
            ORDER BY plan.ordre ASC, plan.nom ASC
            """)
    List<PublicPlanResponse> findSubscribableResponses();
}
