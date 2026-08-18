package org.store.abonnement.application.service;

import org.store.abonnement.application.dto.PlanAbonnementTarifRequest;
import org.store.abonnement.application.dto.PlanAbonnementTarifResponse;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;
import org.store.abonnement.domain.model.TarifAvecCoupon;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPlanAbonnementTarifService {

    /** Retourne le tarif actif pour un plan et une périodicité donnés, ou vide si aucun. */
    Optional<PlanAbonnementTarif> findByPlanAndPeriodicite(PlanAbonnement plan, PeriodiciteAbonnement periodicite);

    /** Retourne tous les tarifs d'un plan sous forme de réponses (actifs ou non). */
    List<PlanAbonnementTarifResponse> findResponsesByPlan(UUID planId);

    /** Lecture interne par id. */
    PlanAbonnementTarif findById(UUID id);

    /**
     * Crée un tarif pour le plan donné. Lève {@code UniqueResourceException} si
     * la même périodicité existe déjà sur ce plan.
     */
    PlanAbonnementTarifResponse create(UUID planId, PlanAbonnementTarifRequest request);

    /**
     * Met à jour un tarif. Revérifie l'unicité périodicité/plan si la périodicité change.
     */
    PlanAbonnementTarifResponse update(UUID planId, UUID tarifId, PlanAbonnementTarifRequest request);

    /** Supprime un tarif. */
    void delete(UUID planId, UUID tarifId);

    /** Retourne les tarifs actifs d'un plan avec leur coupon global applicable (null si aucun), triés par ordre. */
    List<TarifAvecCoupon> findActifWithCoupon(UUID planId);

    /** Lève {@code UniqueResourceException("tarif.periodicite.alreadyExists")} si doublon. */
    void ensurePeriodiciteAvailable(PlanAbonnement plan, PeriodiciteAbonnement periodicite);
}
