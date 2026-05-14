package org.store.stock.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.application.dto.EntreeStockResponse;
import org.store.stock.application.service.IEntreeStockService;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.EntreeStockDomainService;
import org.store.stock.domain.service.MouvementStockDomainService;
import org.store.stock.domain.service.StockDomainService;

/**
 * Orchestre l'entrée stock manuelle : création d'un lot EntreeStock + mise à jour du Stock agrégé + journalisation MouvementStock.
 */
@Service
@Transactional(readOnly = true)
public class EntreeStockServiceImpl implements IEntreeStockService {

    private final EntreeStockDomainService entreeStockDomainService;
    private final StockDomainService stockDomainService;
    private final MouvementStockDomainService mouvementStockDomainService;
    private final IMagasinService magasinService;
    private final IProductFournisseurService productFournisseurService;

    public EntreeStockServiceImpl(EntreeStockDomainService entreeStockDomainService,
                                  StockDomainService stockDomainService,
                                  MouvementStockDomainService mouvementStockDomainService,
                                  IMagasinService magasinService,
                                  IProductFournisseurService productFournisseurService) {
        this.entreeStockDomainService = entreeStockDomainService;
        this.stockDomainService = stockDomainService;
        this.mouvementStockDomainService = mouvementStockDomainService;
        this.magasinService = magasinService;
        this.productFournisseurService = productFournisseurService;
    }

    /** Crée le lot, upsert le stock agrégé (recalcule la moyenne pondérée) et journalise le mouvement ENTREE_ACHAT. */
    @Override
    @Transactional
    public EntreeStockResponse create(EntreeStockRequest entreeStockRequest) {
        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(entreeStockRequest.magasinId()));
        ProductFournisseur productFournisseur = productFournisseurService.ensureBelongsToCurrentEntreprise(
                productFournisseurService.findById(entreeStockRequest.productFournisseurId()));
        Product produit = productFournisseur.getProduct();
        int stockAvant = stockDomainService.findByMagasinIdAndProduitId(magasin.getId(), produit.getId())
                .map(Stock::getQuantiteDisponible)
                .orElse(0);
        EntreeStock entreeStock = entreeStockDomainService.create(entreeStockRequest, magasin, produit, productFournisseur);
        Stock stock = stockDomainService.upsertOnEntry(magasin, produit, entreeStockRequest.quantite(), entreeStockRequest.prixAchat());
        mouvementStockDomainService.journalize(
                stock,
                MouvementStockType.ENTREE_ACHAT,
                entreeStockRequest.quantite(),
                stockAvant,
                stock.getQuantiteDisponible(),
                entreeStockRequest.numeroLot(),
                entreeStockRequest.commentaire()
        );
        return new EntreeStockResponse(entreeStock);
    }
}
