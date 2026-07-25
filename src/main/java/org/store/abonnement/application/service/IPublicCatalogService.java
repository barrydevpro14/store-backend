package org.store.abonnement.application.service;

import org.store.abonnement.application.dto.PublicCatalogResponse;

public interface IPublicCatalogService {

    /** Public catalog (no auth): active + visible plans. */
    PublicCatalogResponse findCatalog();

    /** OWNER subscribable catalog: active + visible + non-trial plans. */
    PublicCatalogResponse findSubscribableCatalog();
}
