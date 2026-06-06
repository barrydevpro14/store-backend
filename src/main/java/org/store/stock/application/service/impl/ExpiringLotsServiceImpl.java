package org.store.stock.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.ExpiringLotResponse;
import org.store.stock.application.dto.ExpiringLotsFilter;
import org.store.stock.application.service.IExpiringLotsService;
import org.store.stock.domain.service.EntreeStockDomainService;

/**
 * Liste les lots EntreeStock qui expirent dans une fenêtre donnée, scopé sur le magasin du caller.
 */
@Service
@Transactional(readOnly = true)
public class ExpiringLotsServiceImpl implements IExpiringLotsService {

    private final EntreeStockDomainService entreeStockDomainService;
    private final IMagasinService magasinService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public ExpiringLotsServiceImpl(EntreeStockDomainService entreeStockDomainService,
                                   IMagasinService magasinService,
                                   ICurrentUserService currentUserService,
                                   ValidatorService validatorService) {
        this.entreeStockDomainService = entreeStockDomainService;
        this.magasinService = magasinService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /** Valide le filter, vérifie l'accès magasin et délègue la query. */
    @Override
    public Page<ExpiringLotResponse> findExpiringLots(ExpiringLotsFilter expiringLotsFilter) {
        validatorService.validate(expiringLotsFilter);
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(expiringLotsFilter.magasinId()));
        return entreeStockDomainService.findExpiringLots(expiringLotsFilter, currentUserService.getCurrent().entrepriseId());
    }
}
