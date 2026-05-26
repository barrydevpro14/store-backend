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
import org.store.stock.application.dto.AjustementStockRequest;
import org.store.stock.application.dto.EntreeStockCreate;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.StockEntryContext;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.service.IAjustementStockService;
import org.store.stock.domain.enums.MotifAjustement;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.enums.TypeAjustement;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.MouvementStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.EntreeStockDomainService;
import org.store.stock.domain.service.MouvementStockDomainService;
import org.store.stock.domain.service.StockDomainService;

import java.util.List;
import java.util.Set;

/**
 * Orchestre l'ajustement manuel du stock (positif ou négatif) avec motif, journalise
 * en MouvementStock(AJUSTEMENT). Positif = mini entrée stock avec fournisseur, Négatif
 * = consommation FIFO sans SortieStock.
 */
@Service
@Transactional(readOnly = true)
public class AjustementStockServiceImpl implements IAjustementStockService {

    private static final Set<MotifAjustement> MOTIFS_POSITIFS = Set.of(MotifAjustement.RETROUVAILLE);
    private static final Set<MotifAjustement> MOTIFS_NEGATIFS = Set.of(MotifAjustement.PERTE, MotifAjustement.CASSE, MotifAjustement.VOL);

    private final EntreeStockDomainService entreeStockDomainService;
    private final StockDomainService stockDomainService;
    private final MouvementStockDomainService mouvementStockDomainService;
    private final IMagasinService magasinService;
    private final IProductService productService;
    private final IProductFournisseurService productFournisseurService;
    private final ICurrentUserService currentUserService;
    private final IAuditEventPublisher auditEventPublisher;

    public AjustementStockServiceImpl(EntreeStockDomainService entreeStockDomainService,
                                      StockDomainService stockDomainService,
                                      MouvementStockDomainService mouvementStockDomainService,
                                      IMagasinService magasinService,
                                      IProductService productService,
                                      IProductFournisseurService productFournisseurService,
                                      ICurrentUserService currentUserService,
                                      IAuditEventPublisher auditEventPublisher) {
        this.entreeStockDomainService = entreeStockDomainService;
        this.stockDomainService = stockDomainService;
        this.mouvementStockDomainService = mouvementStockDomainService;
        this.magasinService = magasinService;
        this.productService = productService;
        this.productFournisseurService = productFournisseurService;
        this.currentUserService = currentUserService;
        this.auditEventPublisher = auditEventPublisher;
    }

    private void auditAdjustment(java.util.UUID entityId, String label) {
        UserPrincipal caller = currentUserService.getCurrent();
        auditEventPublisher.publish(new AuditEvent(AuditAction.STOCK_ADJUSTMENT, AuditEntityType.STOCK, entityId, label,
                caller.accountId().toString(), caller.username(), caller.entrepriseId(), null));
    }

    /** Valide le motif/type, applique l'ajustement (positif ou négatif) et journalise le mouvement. */
    @Override
    @Transactional
    public MouvementStockResponse create(AjustementStockRequest request) {
        validateMotifTypeCoherence(request.type(), request.motif());

        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(request.magasinId()));
        Product produit = productService.ensureBelongsToCurrentEntreprise(productService.findById(request.productId()));

        Stock stock = request.type() == TypeAjustement.POSITIF
                ? applyPositif(request, magasin, produit)
                : applyNegatif(request, magasin, produit);

        MouvementStock mouvement = mouvementStockDomainService.journalize(stock, new MouvementJournalize(
                MouvementStockType.AJUSTEMENT,
                request.quantite(),
                request.type() == TypeAjustement.POSITIF
                        ? stock.getQuantiteDisponible() - request.quantite()
                        : stock.getQuantiteDisponible() + request.quantite(),
                stock.getQuantiteDisponible(),
                request.motif().name(),
                request.commentaire()
        ));

        auditAdjustment(stock.getId(), produit.getNom());
        return new MouvementStockResponse(mouvement);
    }

    /** Crée une mini entrée stock avec fournisseur, upsert le stock agrégé et retourne le stock à jour. */
    public Stock applyPositif(AjustementStockRequest request, Magasin magasin, Product produit) {
        if (request.productFournisseurId() == null || request.prixAchat() == null) {
            throw new BadArgumentException("stock.adjustment.productFournisseurRequired");
        }

        ProductFournisseur productFournisseur = productFournisseurService.ensureBelongsToCurrentEntreprise(
                productFournisseurService.findById(request.productFournisseurId()));
        if (!productFournisseur.getProduct().getId().equals(produit.getId())) {
            throw new BadArgumentException("stock.adjustment.productMismatch");
        }

        entreeStockDomainService.create(new EntreeStockCreate(
                magasin, produit, productFournisseur,
                request.quantite(), request.prixAchat(),
                null, null,
                null));

        return stockDomainService.createOrUpdateEntry(new StockEntryContext(magasin, produit, request.quantite(), request.prixAchat()));
    }

    /** Vérifie la disponibilité, consomme les lots FIFO (sans SortieStock), décrémente le stock et retourne le stock à jour. */
    public Stock applyNegatif(AjustementStockRequest request, Magasin magasin, Product produit) {
        Stock stock = stockDomainService.findByMagasinIdAndProduitId(magasin.getId(), produit.getId())
                .orElseThrow(() -> new EntityException("stock.notFound"));
        if (stock.getQuantiteDisponible() < request.quantite()) {
            throw new BadArgumentException("stock.adjustment.insufficientQuantity",
                    stock.getQuantiteDisponible(), request.quantite());
        }

        List<EntreeStock> lots = entreeStockDomainService.findAvailableLotsForFifo(magasin.getId(), produit.getId());
        consumeLotsFifoForAdjustment(lots, request.quantite());

        return stockDomainService.decrement(stock, request.quantite());
    }

    /** Décrémente quantiteRestante des lots FIFO sans créer de SortieStock. */
    public void consumeLotsFifoForAdjustment(List<EntreeStock> lots, int quantiteDemandee) {
        int[] restant = {quantiteDemandee};

        lots.stream()
                .takeWhile(lot -> restant[0] > 0)
                .forEach(lot -> restant[0] = decrementLot(lot, restant[0]));
    }

    /** Décrémente la quantité restante du lot du minimum entre sa quantité et le restant à consommer, persiste, et retourne le nouveau restant. */
    public int decrementLot(EntreeStock lot, int restant) {
        int aConsommer = Math.min(lot.getQuantiteRestante(), restant);
        lot.setQuantiteRestante(lot.getQuantiteRestante() - aConsommer);
        entreeStockDomainService.save(lot);
        return restant - aConsommer;
    }

    /** Lève BadArgumentException si le motif n'est pas compatible avec le type d'ajustement. */
    public void validateMotifTypeCoherence(TypeAjustement type, MotifAjustement motif) {
        if (type == TypeAjustement.POSITIF && MOTIFS_NEGATIFS.contains(motif)) {
            throw new BadArgumentException("stock.adjustment.motifMismatch", motif.name(), type.name());
        }
        if (type == TypeAjustement.NEGATIF && MOTIFS_POSITIFS.contains(motif)) {
            throw new BadArgumentException("stock.adjustment.motifMismatch", motif.name(), type.name());
        }
    }
}
