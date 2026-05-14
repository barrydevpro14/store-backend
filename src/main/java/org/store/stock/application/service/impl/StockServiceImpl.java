package org.store.stock.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.magasin.application.service.IMagasinService;
import org.store.produit.application.service.IProductService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.application.service.IStockService;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.StockDomainService;

import java.util.UUID;

/**
 * Lecture du stock agrégé (par magasin × produit), scopée par entreprise et restreinte au magasin de l'employé si applicable.
 */
@Service
@Transactional(readOnly = true)
public class StockServiceImpl implements IStockService {

    private final StockDomainService stockDomainService;
    private final IMagasinService magasinService;
    private final IProductService productService;
    private final ICurrentUserService currentUserService;

    public StockServiceImpl(StockDomainService stockDomainService,
                            IMagasinService magasinService,
                            IProductService productService,
                            ICurrentUserService currentUserService) {
        this.stockDomainService = stockDomainService;
        this.magasinService = magasinService;
        this.productService = productService;
        this.currentUserService = currentUserService;
    }

    /** Retourne un stock par id après vérification d'accès (entreprise du caller, magasin propre si employé). */
    @Override
    public StockResponse findResponseById(UUID id) {
        Stock stock = stockDomainService.findById(id);
        magasinService.ensureAccessibleByCurrentUser(stock.getMagasin());
        return new StockResponse(stock);
    }

    /** Liste paginée filtrable par magasin et/ou produit ; force le scope magasin pour les employés sans filtre explicite. */
    @Override
    public Page<StockResponse> findAllByCurrentEntreprise(UUID magasinId, UUID productId, Pageable pageable) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        UUID effectiveMagasinId = magasinId;
        if (effectiveMagasinId != null) {
            magasinService.ensureAccessibleByCurrentUser(magasinService.findById(effectiveMagasinId));
        } else if (currentUser.magasinId() != null) {
            effectiveMagasinId = currentUser.magasinId();
        }
        if (productId != null) {
            productService.ensureBelongsToCurrentEntreprise(productService.findById(productId));
        }
        return stockDomainService.findResponsesByFilters(currentUser.entrepriseId(), effectiveMagasinId, productId, pageable);
    }
}
