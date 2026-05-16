package org.store.stock.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductService;
import org.store.produit.domain.model.Product;
import org.store.stock.application.dto.LotConsumption;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.SortieStockRequest;
import org.store.stock.application.dto.SortieStockResponse;
import org.store.stock.application.service.ISortieStockService;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.SortieStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.EntreeStockDomainService;
import org.store.stock.domain.service.MouvementStockDomainService;
import org.store.stock.domain.service.SortieStockDomainService;
import org.store.stock.domain.service.StockDomainService;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestre la sortie stock FIFO : consomme les lots du plus ancien au plus récent,
 * crée une SortieStock par lot consommé, décrémente le stock agrégé et journalise le mouvement.
 */
@Service
@Transactional(readOnly = true)
public class SortieStockServiceImpl implements ISortieStockService {

    private final EntreeStockDomainService entreeStockDomainService;
    private final SortieStockDomainService sortieStockDomainService;
    private final StockDomainService stockDomainService;
    private final MouvementStockDomainService mouvementStockDomainService;
    private final IMagasinService magasinService;
    private final IProductService productService;

    public SortieStockServiceImpl(EntreeStockDomainService entreeStockDomainService,
                                  SortieStockDomainService sortieStockDomainService,
                                  StockDomainService stockDomainService,
                                  MouvementStockDomainService mouvementStockDomainService,
                                  IMagasinService magasinService,
                                  IProductService productService) {
        this.entreeStockDomainService = entreeStockDomainService;
        this.sortieStockDomainService = sortieStockDomainService;
        this.stockDomainService = stockDomainService;
        this.mouvementStockDomainService = mouvementStockDomainService;
        this.magasinService = magasinService;
        this.productService = productService;
    }

    /** Vérifie les accès, consomme les lots FIFO, met à jour stock agrégé et journalise. */
    @Override
    @Transactional
    public List<SortieStockResponse> create(SortieStockRequest sortieStockRequest) {
        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(sortieStockRequest.magasinId()));
        Product produit = productService.ensureBelongsToCurrentEntreprise(productService.findById(sortieStockRequest.productId()));

        Stock stock = stockDomainService.findByMagasinIdAndProduitId(magasin.getId(), produit.getId())
                .orElseThrow(() -> new EntityException("stock.notFound"));
        int stockAvant = stock.getQuantiteDisponible();
        if (stockAvant < sortieStockRequest.quantite()) {
            throw new BadArgumentException("stock.exit.insufficientQuantity", stockAvant, sortieStockRequest.quantite());
        }

        List<EntreeStock> lots = entreeStockDomainService.findAvailableLotsForFifo(magasin.getId(), produit.getId());
        List<SortieStockResponse> sorties = consumeLotsFifo(lots, sortieStockRequest.quantite(), sortieStockRequest.prixVente());

        Stock updated = stockDomainService.decrement(stock, sortieStockRequest.quantite());

        mouvementStockDomainService.journalize(updated, new MouvementJournalize(
                MouvementStockType.SORTIE_VENTE,
                sortieStockRequest.quantite(),
                stockAvant,
                updated.getQuantiteDisponible(),
                null,
                sortieStockRequest.commentaire()
        ));

        return sorties;
    }

    /** Consomme les lots FIFO dans l'ordre jusqu'à atteindre la quantité demandée et retourne les sorties créées. */
    public List<SortieStockResponse> consumeLotsFifo(List<EntreeStock> lots, int quantiteDemandee, java.math.BigDecimal prixVente) {
        List<SortieStockResponse> sorties = new ArrayList<>();
        int restant = quantiteDemandee;

        for (EntreeStock lot : lots) {
            if (restant == 0) break;
            LotConsumption consumption = consumeLot(lot, restant, prixVente);
            sorties.add(consumption.sortie());
            restant = consumption.restantApres();
        }
        return sorties;
    }

    /** Décrémente le lot du minimum entre sa quantité restante et le restant à consommer, persiste, crée la SortieStock et retourne le nouveau restant. */
    public LotConsumption consumeLot(EntreeStock lot, int restant, java.math.BigDecimal prixVente) {
        int aConsommer = Math.min(lot.getQuantiteRestante(), restant);

        lot.setQuantiteRestante(lot.getQuantiteRestante() - aConsommer);
        entreeStockDomainService.save(lot);

        SortieStock sortie = sortieStockDomainService.create(lot, aConsommer, prixVente);
        return new LotConsumption(new SortieStockResponse(sortie), restant - aConsommer);
    }
}
