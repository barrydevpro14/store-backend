package org.store.abonnement.application.service;

import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;

import java.util.List;
import java.util.Optional;

public interface IPlanAbonnementTarifService {

    /**
     * Retourne le tarif actif pour un plan et une périodicité donnés, ou vide si aucun.
     */
    Optional<PlanAbonnementTarif> findByPlanAndPeriodicite(PlanAbonnement plan, PeriodiciteAbonnement periodicite);

    /**
     * Retourne tous les tarifs d'un plan (actifs ou non).
     */
    List<PlanAbonnementTarif> findByPlan(PlanAbonnement plan);
}
