package org.store.stock.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.MarginReportFilter;
import org.store.stock.application.dto.MarginReportResponse;
import org.store.stock.application.service.IMarginReportService;
import org.store.stock.domain.service.SortieStockDomainService;

/**
 * Calcule le reporting de marges réelles agrégé à partir du journal SortieStock,
 * scopé sur le magasin du caller.
 */
@Service
@Transactional(readOnly = true)
public class MarginReportServiceImpl implements IMarginReportService {

    private final SortieStockDomainService sortieStockDomainService;
    private final IMagasinService magasinService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public MarginReportServiceImpl(SortieStockDomainService sortieStockDomainService,
                                   IMagasinService magasinService,
                                   ICurrentUserService currentUserService,
                                   ValidatorService validatorService) {
        this.sortieStockDomainService = sortieStockDomainService;
        this.magasinService = magasinService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /** Valide le filter, vérifie l'accès au magasin et délègue le calcul agrégé. */
    @Override
    public MarginReportResponse compute(MarginReportFilter marginReportFilter) {
        validatorService.validate(marginReportFilter);
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(marginReportFilter.magasinId()));
        return sortieStockDomainService.computeMargin(marginReportFilter, currentUserService.getCurrent().entrepriseId());
    }
}
