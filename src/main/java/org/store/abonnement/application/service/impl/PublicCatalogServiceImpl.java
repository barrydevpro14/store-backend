package org.store.abonnement.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.PublicCatalogResponse;
import org.store.abonnement.application.service.IPublicCatalogService;
import org.store.abonnement.domain.service.PlanAbonnementDomainService;

/**
 * Builds the public subscription catalog (plans only) for the public landing page and OWNER subscribe flow.
 */
@Service
@Transactional(readOnly = true)
public class PublicCatalogServiceImpl implements IPublicCatalogService {

    private final PlanAbonnementDomainService planAbonnementDomainService;

    public PublicCatalogServiceImpl(PlanAbonnementDomainService planAbonnementDomainService) {
        this.planAbonnementDomainService = planAbonnementDomainService;
    }

    /** Returns all active + visible plans (including trial) for the public landing. */
    @Override
    public PublicCatalogResponse findCatalog() {
        return new PublicCatalogResponse(planAbonnementDomainService.findPublicResponses());
    }

    /** Returns active + visible + non-trial plans for the OWNER subscribe screen. */
    @Override
    public PublicCatalogResponse findSubscribableCatalog() {
        return new PublicCatalogResponse(planAbonnementDomainService.findSubscribableResponses());
    }
}
