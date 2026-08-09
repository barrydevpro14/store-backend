package org.store.stock.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.common.exceptions.BadArgumentException;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.AjustementStockRequest;
import org.store.stock.application.dto.EntreeStockCreate;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.stock.application.dto.StockEntryContext;
import org.store.stock.application.service.IAjustementStockService;
import org.store.stock.application.service.IEntreeStockService;
import org.store.stock.application.service.IMouvementStockService;
import org.store.stock.application.service.IStockService;
import org.store.stock.domain.enums.MotifAjustement;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.enums.TypeAjustement;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.Stock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestre l'ajustement manuel du stock (positif ou négatif) avec motif, journalise
 * en MouvementStock(AJUSTEMENT). Positif = mini entrée stock au prix du PF existant,
 * Négatif = consommation FIFO sans SortieStock. Le fournisseur et le prix sont dérivés
 * du stock existant — aucune saisie supplémentaire côté appelant.
 */
@Service
@Transactional(readOnly = true)
public class AjustementStockServiceImpl implements IAjustementStockService {

    private static final Set<MotifAjustement> MOTIFS_POSITIFS = Set.of(MotifAjustement.RETROUVAILLE);
    private static final Set<MotifAjustement> MOTIFS_NEGATIFS = Set.of(MotifAjustement.PERTE, MotifAjustement.CASSE, MotifAjustement.VOL);

    private final IEntreeStockService entreeStockService;
    private final IStockService stockService;
    private final IMouvementStockService mouvementStockService;
    private final IMagasinService magasinService;
    private final ICurrentUserService currentUserService;
    private final IAuditEventPublisher auditEventPublisher;

    public AjustementStockServiceImpl(IEntreeStockService entreeStockService,
                                      IStockService stockService,
                                      IMouvementStockService mouvementStockService,
                                      IMagasinService magasinService,
                                      ICurrentUserService currentUserService,
                                      IAuditEventPublisher auditEventPublisher) {
        this.entreeStockService = entreeStockService;
        this.stockService = stockService;
        this.mouvementStockService = mouvementStockService;
        this.magasinService = magasinService;
        this.currentUserService = currentUserService;
        this.auditEventPublisher = auditEventPublisher;
    }

    /** Valide le motif/type, applique l'ajustement (positif ou négatif) et journalise le mouvement. */
    @Override
    @Transactional
    public MouvementStockResponse create(AjustementStockRequest request) {
        validateMotifTypeCoherence(request.type(), request.motif(), request.commentaire());

        Stock stock = stockService.findById(request.stockId());
        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(stock.getMagasin());
        ProductFournisseur pf = stock.getProductFournisseur();

        Stock updatedStock = request.type() == TypeAjustement.POSITIF
                ? applyPositif(request, magasin, pf)
                : applyNegatif(request, stock, pf);

        MouvementStockResponse mouvement = mouvementStockService.journalize(updatedStock, new MouvementJournalize(
                MouvementStockType.AJUSTEMENT,
                request.type() == TypeAjustement.POSITIF ? request.quantite() : request.quantite().negate(),
                request.type() == TypeAjustement.POSITIF
                        ? updatedStock.getQuantiteDisponible().subtract(request.quantite())
                        : updatedStock.getQuantiteDisponible().add(request.quantite()),
                updatedStock.getQuantiteDisponible(),
                request.motif().name(),
                request.commentaire()
        ));

        auditAdjustment(updatedStock.getId(), pf.getProduct().getNom(), magasin.getId());
        return mouvement;
    }

    /** Crée une mini entrée stock au prix d'achat du PF, upsert le stock agrégé et retourne le stock à jour. */
    public Stock applyPositif(AjustementStockRequest request, Magasin magasin, ProductFournisseur pf) {
        entreeStockService.createEntreeStock(new EntreeStockCreate(
                magasin, pf.getProduct(), pf,
                request.quantite(), pf.getPrixAchat(),
                null, null, null));

        return stockService.createOrUpdateEntry(new StockEntryContext(magasin, pf, request.quantite(), pf.getPrixAchat()));
    }

    /**
     * Charge les lots FIFO du PF, vérifie la disponibilité réelle, consomme les lots
     * (sans SortieStock), décrémente le stock et retourne le stock à jour.
     */
    public Stock applyNegatif(AjustementStockRequest request, Stock stock, ProductFournisseur pf) {
        List<EntreeStock> lots = entreeStockService.findAvailableLotsForFifo(stock.getMagasin().getId(), pf.getId());
        BigDecimal disponibleLots = lots.stream()
                .map(EntreeStock::getQuantiteRestante)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (disponibleLots.compareTo(request.quantite()) < 0) {
            throw new BadArgumentException("stock.adjustment.insufficientQuantity",
                    disponibleLots, request.quantite());
        }

        consumeLotsFifoForAdjustment(lots, request.quantite());
        return stockService.decrement(stock, request.quantite());
    }

    /** Décrémente quantiteRestante des lots FIFO sans créer de SortieStock. */
    public void consumeLotsFifoForAdjustment(List<EntreeStock> lots, BigDecimal quantiteDemandee) {
        BigDecimal[] restant = {quantiteDemandee};

        lots.stream()
                .takeWhile(lot -> restant[0].compareTo(BigDecimal.ZERO) > 0)
                .forEach(lot -> restant[0] = decrementLot(lot, restant[0]));
    }

    /** Décrémente la quantité restante du lot du minimum entre sa quantité et le restant à consommer, persiste, et retourne le nouveau restant. */
    public BigDecimal decrementLot(EntreeStock lot, BigDecimal restant) {
        BigDecimal aConsommer = lot.getQuantiteRestante().min(restant);
        lot.setQuantiteRestante(lot.getQuantiteRestante().subtract(aConsommer));
        entreeStockService.saveLot(lot);
        return restant.subtract(aConsommer);
    }

    /** Lève BadArgumentException si le motif n'est pas compatible avec le type d'ajustement, ou si AUTRE sans commentaire. */
    public void validateMotifTypeCoherence(TypeAjustement type, MotifAjustement motif, String commentaire) {
        if (type == TypeAjustement.POSITIF && MOTIFS_NEGATIFS.contains(motif)) {
            throw new BadArgumentException("stock.adjustment.motifMismatch", motif.name(), type.name());
        }
        if (type == TypeAjustement.NEGATIF && MOTIFS_POSITIFS.contains(motif)) {
            throw new BadArgumentException("stock.adjustment.motifMismatch", motif.name(), type.name());
        }
        if (motif == MotifAjustement.AUTRE && (commentaire == null || commentaire.isBlank())) {
            throw new BadArgumentException("stock.adjustment.commentaireRequiredForAutre");
        }
    }

    private void auditAdjustment(UUID entityId, String label, UUID magasinId) {
        UserPrincipal caller = currentUserService.getCurrent();
        auditEventPublisher.publish(new AuditEvent(AuditAction.STOCK_ADJUSTMENT, AuditEntityType.STOCK, entityId, label,
                caller.accountId().toString(), caller.username(), caller.entrepriseId(), magasinId, null));
    }
}
