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

    Optional<PlanAbonnement> findFirstByTrialTrueAndActifTrue();

    boolean existsByNom(String nom);

    @Query("""
            SELECT new org.store.abonnement.application.dto.PlanAbonnementResponse(plan)
            FROM PlanAbonnement plan
            WHERE (:#{#filter.nom}     IS NULL OR LOWER(plan.nom) LIKE LOWER(CONCAT('%', :#{#filter.nom}, '%')))
              AND (:#{#filter.actif}   IS NULL OR plan.actif   = :#{#filter.actif})
              AND (:#{#filter.visible} IS NULL OR plan.visible = :#{#filter.visible})
              AND (:#{#filter.trial}   IS NULL OR plan.trial   = :#{#filter.trial})
            """)
    Page<PlanAbonnementResponse> findResponsesByFilter(@Param("filter") PlanAbonnementFilter filter, Pageable pageable);

    @Query("""
            SELECT new org.store.abonnement.application.dto.PublicPlanResponse(
                    plan.id, plan.nom, plan.description, plan.prix,
                    plan.nombreMagasinsMax, plan.nombreEmployesMax,
                    plan.gestionStock, plan.gestionVente, plan.gestionAchat, plan.gestionComptabilite,
                    plan.trial, plan.ordre)
            FROM PlanAbonnement plan
            WHERE plan.actif = true AND plan.visible = true
            ORDER BY plan.ordre ASC, plan.nom ASC
            """)
    List<PublicPlanResponse> findPublicResponses();
}
