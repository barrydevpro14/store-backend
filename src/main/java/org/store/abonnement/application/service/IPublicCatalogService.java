package org.store.abonnement.application.service;

import org.store.abonnement.application.dto.PublicCatalogResponse;
import org.store.abonnement.application.dto.PublicPlanResponse;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public interface IPublicCatalogService {

    /**
     * Catalogue public (sans authentification) pour la landing de souscription.
     * Agrège : plans visibles+actifs (avec leurs promotions imbriquées), types d'abonnement actifs, promotions globales (sans plan).
     */
    PublicCatalogResponse findCatalog();

    /**
     * Sous-catalogue OWNER : uniquement les plans souscriptibles (≥ 1 type actif non-trial), avec
     * leurs durées non-trial. Sert l'écran `/dashboard/entreprise/abonnement/souscrire`. Le plan
     * d'essai et le type d'essai sont écartés au niveau JPQL — pas de filtrage côté front.
     */
    PublicCatalogResponse findSubscribableCatalog();

    /** Hydrate les plans avec leurs promotions et types via le loader fourni, et agrège les promotions globales. */
    PublicCatalogResponse buildCatalog(List<PublicPlanResponse> plansBase, Function<UUID, List<SubscriptionTypeResponse>> typeLoader);

    /** Retourne les types actifs d'un plan (tous, y compris trial). */
    List<SubscriptionTypeResponse> typesForPlan(UUID planId);

    /** Retourne les types actifs non-trial d'un plan — pour le catalogue souscriptible OWNER. */
    List<SubscriptionTypeResponse> nonTrialTypesForPlan(UUID planId);
}
