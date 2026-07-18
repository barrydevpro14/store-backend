package org.store.stock.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.BelowThresholdFilter;
import org.store.stock.application.dto.StockEntryContext;
import org.store.stock.application.dto.StockFilter;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.application.dto.StockThresholdRequest;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.application.service.IStockService;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.StockDomainService;

import java.util.Optional;
import java.util.UUID;

/**
 * Lecture du stock agrégé (par magasin × produit), scopée par entreprise du caller.
 * Configure aussi le seuil d'approvisionnement et liste les stocks en alerte.
 */
@Service
@Transactional(readOnly = true)
public class StockServiceImpl implements IStockService {

    private final StockDomainService stockDomainService;
    private final IMagasinService magasinService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public StockServiceImpl(StockDomainService stockDomainService,
                            IMagasinService magasinService,
                            ICurrentUserService currentUserService,
                            ValidatorService validatorService) {
        this.stockDomainService = stockDomainService;
        this.magasinService = magasinService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /** Retourne un stock par id après vérification d'accès (entreprise du caller, magasin propre si employé). */
    @Override
    public StockResponse findResponseById(UUID id) {
        Stock stock = stockDomainService.findById(id);
        magasinService.ensureAccessibleByCurrentUser(stock.getMagasin());
        return new StockResponse(stock);
    }

    /** Valide le filter puis délègue la query scopée par entreprise du caller. */
    @Override
    public Page<StockResponse> findAllByCurrentEntreprise(StockFilter filter) {
        validatorService.validate(filter);
        return stockDomainService.findResponsesByFilter(filter, currentUserService.getCurrent().entrepriseId());
    }

    /** Valide le filter puis délègue la query "below threshold" scopée par entreprise du caller. */
    @Override
    public Page<StockResponse> findBelowThresholdByCurrentEntreprise(BelowThresholdFilter filter) {
        validatorService.validate(filter);
        return stockDomainService.findResponsesBelowThreshold(filter, currentUserService.getCurrent().entrepriseId());
    }

    /** Met à jour le seuil d'approvisionnement après vérification d'accès magasin. */
    @Override
    @Transactional
    public StockResponse updateThreshold(UUID id, StockThresholdRequest stockThresholdRequest) {
        validatorService.validate(stockThresholdRequest);
        Stock stock = stockDomainService.findById(id);
        magasinService.ensureAccessibleByCurrentUser(stock.getMagasin());
        Stock updated = stockDomainService.updateThreshold(stock, stockThresholdRequest.seuilApprovisionnement());
        return new StockResponse(updated);
    }

    /** Compte les stocks sous seuil après vérification d'accès magasin. */
    @Override
    public long countBelowThresholdByCurrentEntreprise(UUID magasinId) {
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(magasinId));
        return stockDomainService.countBelowThreshold(magasinId);
    }

    /** Calcule la valorisation après vérification d'accès magasin. */
    @Override
    public StockValuationResponse computeValuation(UUID magasinId) {
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(magasinId));
        return stockDomainService.computeValuation(magasinId, currentUserService.getCurrent().entrepriseId());
    }

    @Override
    @Transactional
    public Stock createOrUpdateEntry(StockEntryContext context) {
        return stockDomainService.createOrUpdateEntry(context);
    }

    @Override
    public Optional<Stock> findByMagasinAndProductFournisseur(UUID magasinId, UUID productFournisseurId) {
        return stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, productFournisseurId);
    }

    @Override
    @Transactional
    public Stock decrement(Stock stock, int quantite) {
        return stockDomainService.decrement(stock, quantite);
    }
}
