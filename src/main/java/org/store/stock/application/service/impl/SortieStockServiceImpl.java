package org.store.stock.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.application.service.IProductService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.stock.application.dto.LotConsumption;
import org.store.stock.application.dto.LotConsumptionContext;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.SortieStockCreate;
import org.store.stock.application.dto.SortieStockForVente;
import org.store.stock.application.dto.SortieStockRequest;
import org.store.stock.application.dto.SortieStockResponse;
import org.store.stock.application.service.ISortieStockService;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.SortieStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.EntreeStockDomainService;
import org.store.notification.application.event.StockBelowThresholdEvent;
import org.store.notification.application.service.INotificationEventPublisher;
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
    private final IProductFournisseurService productFournisseurService;
    private final INotificationEventPublisher notificationEventPublisher;

    public SortieStockServiceImpl(EntreeStockDomainService entreeStockDomainService,
                                  SortieStockDomainService sortieStockDomainService,
                                  StockDomainService stockDomainService,
                                  MouvementStockDomainService mouvementStockDomainService,
                                  IMagasinService magasinService,
                                  IProductFournisseurService productFournisseurService,
                                  INotificationEventPublisher notificationEventPublisher) {
        this.entreeStockDomainService = entreeStockDomainService;
        this.sortieStockDomainService = sortieStockDomainService;
        this.stockDomainService = stockDomainService;
        this.mouvementStockDomainService = mouvementStockDomainService;
        this.magasinService = magasinService;
        this.productFournisseurService = productFournisseurService;
        this.notificationEventPublisher = notificationEventPublisher;
    }

    /** Vérifie les accès, consomme les lots FIFO, met à jour stock agrégé et journalise. */
    @Override
    @Transactional
    public List<SortieStockResponse> create(SortieStockRequest sortieStockRequest) {
        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(sortieStockRequest.magasinId()));
        ProductFournisseur productFournisseur = productFournisseurService.ensureBelongsToCurrentEntreprise(
                productFournisseurService.findById(sortieStockRequest.productFournisseurId()));

        Stock stock = stockDomainService.findByMagasinIdAndProductFournisseurId(magasin.getId(), productFournisseur.getId())
                .orElseThrow(() -> new EntityException("stock.notFound"));
        int stockAvant = stock.getQuantiteDisponible();
        if (stockAvant < sortieStockRequest.quantite()) {
            throw new BadArgumentException("stock.exit.insufficientQuantity", stockAvant, sortieStockRequest.quantite());
        }

        List<EntreeStock> lots = entreeStockDomainService.findAvailableLotsForFifoByProductFournisseur(magasin.getId(), productFournisseur.getId());
        List<SortieStockResponse> sorties = consumeFifo(lots,
                new LotConsumptionContext(sortieStockRequest.quantite(), sortieStockRequest.prixVente(), null));

        Stock updated = stockDomainService.decrement(stock, sortieStockRequest.quantite());

        if (updated.getSeuilApprovisionnement() > 0 && updated.getQuantiteDisponible() <= updated.getSeuilApprovisionnement()) {
            notificationEventPublisher.publishStockBelowThreshold(new StockBelowThresholdEvent(updated));
        }

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

    /** Consomme les lots FIFO du ProductFournisseur ciblé pour une ligne de vente : vérifie stock, journalise et lie les sorties à la ligne. */
    @Override
    @Transactional
    public List<SortieStockResponse> consumeForVente(SortieStockForVente sortieStockForVente) {
        Magasin magasin = sortieStockForVente.magasin();
        ProductFournisseur productFournisseur = sortieStockForVente.productFournisseur();
        Product produit = productFournisseur.getProduct();

        Stock stock = stockDomainService.findByMagasinIdAndProductFournisseurId(magasin.getId(), productFournisseur.getId())
                .orElseThrow(() -> new EntityException("stock.notFound"));
        int stockAvant = stock.getQuantiteDisponible();

        List<EntreeStock> lots = entreeStockDomainService.findAvailableLotsForFifoByProductFournisseur(magasin.getId(), productFournisseur.getId());
        int totalDisponible = lots.stream().mapToInt(EntreeStock::getQuantiteRestante).sum();
        if (totalDisponible < sortieStockForVente.quantite()) {
            throw new BadArgumentException("stock.exit.insufficientQuantity", totalDisponible, sortieStockForVente.quantite());
        }

        List<SortieStockResponse> sorties = consumeFifo(lots, new LotConsumptionContext(
                sortieStockForVente.quantite(), sortieStockForVente.prixVente(), sortieStockForVente.ligneVente()
        ));

        Stock updated = stockDomainService.decrement(stock, sortieStockForVente.quantite());

        if (updated.getSeuilApprovisionnement() > 0 && updated.getQuantiteDisponible() <= updated.getSeuilApprovisionnement()) {
            notificationEventPublisher.publishStockBelowThreshold(new StockBelowThresholdEvent(updated));
        }

        mouvementStockDomainService.journalize(updated, new MouvementJournalize(
                MouvementStockType.SORTIE_VENTE,
                sortieStockForVente.quantite(),
                stockAvant,
                updated.getQuantiteDisponible(),
                null,
                null
        ));

        return sorties;
    }

    /** Consomme les lots FIFO selon le contexte (qty cible, prix, ligneVente optionnelle) et retourne les sorties créées. */
    public List<SortieStockResponse> consumeFifo(List<EntreeStock> lots, LotConsumptionContext context) {
        List<SortieStockResponse> sorties = new ArrayList<>();
        int[] restant = {context.totalAConsommer()};

        lots.stream()
                .takeWhile(lot -> restant[0] > 0)
                .forEach(lot -> {
                    LotConsumption consumption = consumeOneLot(lot, restant[0], context);
                    sorties.add(consumption.sortie());
                    restant[0] = consumption.restantApres();
                });

        return sorties;
    }

    /** Décrémente le lot, persiste, crée la SortieStock (avec ligneVente eventuelle) et retourne le nouveau restant. */
    public LotConsumption consumeOneLot(EntreeStock lot, int restant, LotConsumptionContext context) {
        int aConsommer = Math.min(lot.getQuantiteRestante(), restant);

        lot.setQuantiteRestante(lot.getQuantiteRestante() - aConsommer);
        entreeStockDomainService.save(lot);

        SortieStock sortie = sortieStockDomainService.create(new SortieStockCreate(
                lot, aConsommer, context.prixVente(), context.ligneVente()
        ));
        return new LotConsumption(new SortieStockResponse(sortie), restant - aConsommer);
    }
}
