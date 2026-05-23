package org.store.abonnement.application.service;

import org.store.abonnement.application.dto.PublicCatalogResponse;

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
}
