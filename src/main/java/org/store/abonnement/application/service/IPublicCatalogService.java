package org.store.abonnement.application.service;

import org.store.abonnement.application.dto.PublicCatalogResponse;

public interface IPublicCatalogService {

    /**
     * Catalogue public (sans authentification) pour la landing de souscription.
     * Agrège : plans visibles+actifs (avec leurs promotions imbriquées), types d'abonnement actifs, promotions globales (sans plan).
     */
    PublicCatalogResponse findCatalog();
}
