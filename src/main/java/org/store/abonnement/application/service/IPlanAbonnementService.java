package org.store.abonnement.application.service;

import org.springframework.data.domain.Page;
import org.store.abonnement.application.dto.PlanAbonnementFilter;
import org.store.abonnement.application.dto.PlanAbonnementRequest;
import org.store.abonnement.application.dto.PlanAbonnementResponse;
import org.store.abonnement.domain.model.PlanAbonnement;

import java.util.UUID;

public interface IPlanAbonnementService {

    /**
     * Retourne le premier plan d'essai actif, sinon throw `EntityException("plan.trial.notFound")`.
     */
    PlanAbonnement findFirstTrialActif();

    /**
     * Création d'un plan d'abonnement (ADMIN uniquement). Unicité du nom contrôlée.
     */
    PlanAbonnementResponse create(PlanAbonnementRequest planAbonnementRequest);

    /**
     * Lecture interne par id (utilisée par d'autres agrégats : Abonnement, Coupon, Promotion).
     */
    PlanAbonnement findById(UUID id);

    /**
     * Variante null-safe : retourne `null` si l'`id` est `null`, sinon délègue à `findById`.
     * Utilisée pour les FK optionnelles (`Coupon.plan`, `Promotion.plan` sont nullables).
     */
    default PlanAbonnement findByIdOrNull(UUID id) {
        return id == null ? null : findById(id);
    }

    /**
     * Lecture par id en `Response`. Throw `EntityException("plan.notFound")` si introuvable.
     */
    PlanAbonnementResponse findResponseById(UUID id);

    /**
     * Listing paginé filtré (nom, actif, visible, trial).
     */
    Page<PlanAbonnementResponse> findAll(PlanAbonnementFilter filter);

    /**
     * Mise à jour d'un plan existant. Unicité du nom revérifiée si changement.
     */
    PlanAbonnementResponse update(UUID id, PlanAbonnementRequest planAbonnementRequest);

    /**
     * Activation d'un plan (setter `actif=true`).
     */
    PlanAbonnementResponse activate(UUID id);

    /**
     * Désactivation d'un plan (setter `actif=false`). Le plan reste référencé par les abonnements existants.
     */
    PlanAbonnementResponse deactivate(UUID id);

    /**
     * Suppression d'un plan. Peut échouer en BDD si référencé par un abonnement (ON DELETE RESTRICT).
     */
    void delete(UUID id);

    /**
     * Vérifie qu'aucun plan ne porte déjà ce nom. Throw `UniqueResourceException("plan.nom.alreadyExists")` sinon.
     */
    void ensureNomAvailable(String nom);
}
