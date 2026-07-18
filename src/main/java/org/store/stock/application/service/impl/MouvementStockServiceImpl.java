package org.store.stock.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.service.ValidatorService;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.MouvementStockFilter;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.stock.application.service.IMouvementStockService;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.MouvementStockDomainService;

/**
 * Lecture du journal des mouvements de stock, scopée par entreprise du caller.
 */
@Service
@Transactional(readOnly = true)
public class MouvementStockServiceImpl implements IMouvementStockService {

    private final MouvementStockDomainService mouvementStockDomainService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public MouvementStockServiceImpl(MouvementStockDomainService mouvementStockDomainService,
                                     ICurrentUserService currentUserService,
                                     ValidatorService validatorService) {
        this.mouvementStockDomainService = mouvementStockDomainService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /** Valide le filter puis délègue la query scopée par entreprise du caller. */
    @Override
    public Page<MouvementStockResponse> findAllByCurrentEntreprise(MouvementStockFilter filter) {
        validatorService.validate(filter);
        return mouvementStockDomainService.findResponsesByFilter(filter, currentUserService.getCurrent().entrepriseId());
    }

    @Override
    @Transactional
    public void journalize(Stock stock, MouvementJournalize mouvementJournalize) {
        mouvementStockDomainService.journalize(stock, mouvementJournalize);
    }
}
