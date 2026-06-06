package org.store.abonnement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.abonnement.application.dto.PlanAbonnementFilter;
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
            WHERE (:#{#filter.nom}     IS NULL OR LOWER(plan.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%')))
              AND (:#{#filter.actif}   IS NULL OR plan.actif   = :#{#filter.actif})
              AND (:#{#filter.visible} IS NULL OR plan.visible = :#{#filter.visible})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR plan.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR plan.createdAt <  :#{#filter.createdEndDateTime()})
            ORDER BY plan.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(plan)
            FROM PlanAbonnement plan
            WHERE (:#{#filter.nom}     IS NULL OR LOWER(plan.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%')))
              AND (:#{#filter.actif}   IS NULL OR plan.actif   = :#{#filter.actif})
              AND (:#{#filter.visible} IS NULL OR plan.visible = :#{#filter.visible})
              AND (:#{#filter.createdStartDateTime()} IS NULL OR plan.createdAt >= :#{#filter.createdStartDateTime()})
              AND (:#{#filter.createdEndDateTime()}   IS NULL OR plan.createdAt <  :#{#filter.createdEndDateTime()})
            """)
    Page<PlanAbonnementResponse> findResponsesByFilter(@Param("filter") PlanAbonnementFilter filter, Pageable pageable);

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
