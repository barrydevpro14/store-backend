package org.store.abonnement.application.service;

import org.springframework.data.domain.Page;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.application.dto.CurrentAbonnementResponse;
import org.store.abonnement.application.dto.RenouvellementAutoRequest;
import org.store.abonnement.application.dto.SubscribeRequest;
import org.store.abonnement.application.dto.SubscribeResponse;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.entreprise.domain.model.Entreprise;

import java.util.UUID;

public interface IAbonnementService {

    /**
     * Création interne d'un abonnement d'essai (utilisé par le flux d'inscription propriétaire).
     */
    Abonnement createTrial(Entreprise entreprise, PlanAbonnement plan);

    /**
     * Souscription OWNER : crée un Abonnement EN_ATTENTE pour l'entreprise du caller.
     */
    SubscribeResponse subscribe(SubscribeRequest subscribeRequest);

    /**
     * Lecture interne par id.
     */
    Abonnement findById(UUID id);

    /**
     * Active ou désactive le renouvellement automatique sur un abonnement de l'entreprise du caller.
     */
    AbonnementResponse updateRenouvellementAuto(UUID abonnementId, RenouvellementAutoRequest request);

    /**
     * Listing paginé filtré (ADMIN) : tous abonnements par entreprise / statut / plan.
     */
    Page<AbonnementResponse> findAll(AbonnementFilter filter);

    /**
     * Historique paginé du OWNER : ses abonnements (auto-scopé entreprise du caller).
     */
    Page<AbonnementResponse> findMyHistory(AbonnementFilter filter);

    /**
     * Abonnement courant ACTIF du OWNER + jours restants + flag trial + fonctionnalités du plan.
     * Throw `EntityException("abonnement.noActive")` si aucun abonnement actif.
     */
    CurrentAbonnementResponse findMyCurrent();

    /**
     * Vérifie qu'un plan est souscriptible : actif, visible, non-trial.
     */
    void ensurePlanSubscribable(PlanAbonnement plan);

    /**
     * Vérifie que l'abonnement appartient à l'entreprise du caller.
     */
    Abonnement ensureBelongsToCurrentEntreprise(Abonnement abonnement);
}
