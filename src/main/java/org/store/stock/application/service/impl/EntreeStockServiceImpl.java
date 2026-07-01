package org.store.stock.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.service.IFournisseurService;
import org.store.achat.domain.model.Fournisseur;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.dto.ProductFournisseurRequest;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.EntreeStockCreate;
import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.application.dto.EntreeStockResponse;
import org.store.stock.application.dto.LigneEntreeStockRequest;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.StockEntryContext;
import org.store.stock.application.service.IEntreeStockService;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.EntreeStockDomainService;
import org.store.stock.domain.service.MouvementStockDomainService;
import org.store.stock.domain.service.StockDomainService;

import java.util.List;
import java.util.UUID;

/**
 * Orchestre l'enregistrement d'entrées stock multi-lignes pour un même fournisseur, sans
 * passer par une {@code CommandeAchat}. Cas d'usage : démarrage d'un magasin qui possède
 * déjà un stock physique au moment de la bascule sur l'application. Pour chaque ligne :
 * findOrCreate du {@code ProductFournisseur}, création du lot {@code EntreeStock} (sans
 * commande), upsert du {@code Stock} agrégé (PMP) et journalisation {@code ENTREE_INITIAL}.
 * Un seul {@code AuditEvent STOCK_INITIAL_ENTRY} global est publié en fin d'opération.
 */
@Service
@Transactional(readOnly = true)
public class EntreeStockServiceImpl implements IEntreeStockService {

    private final EntreeStockDomainService entreeStockDomainService;
    private final StockDomainService stockDomainService;
    private final MouvementStockDomainService mouvementStockDomainService;
    private final IMagasinService magasinService;
    private final IFournisseurService fournisseurService;
    private final IProductFournisseurService productFournisseurService;
    private final ICurrentUserService currentUserService;
    private final IAuditEventPublisher auditEventPublisher;

    public EntreeStockServiceImpl(EntreeStockDomainService entreeStockDomainService,
                                  StockDomainService stockDomainService,
                                  MouvementStockDomainService mouvementStockDomainService,
                                  IMagasinService magasinService,
                                  IFournisseurService fournisseurService,
                                  IProductFournisseurService productFournisseurService,
                                  ICurrentUserService currentUserService,
                                  IAuditEventPublisher auditEventPublisher) {
        this.entreeStockDomainService = entreeStockDomainService;
        this.stockDomainService = stockDomainService;
        this.mouvementStockDomainService = mouvementStockDomainService;
        this.magasinService = magasinService;
        this.fournisseurService = fournisseurService;
        this.productFournisseurService = productFournisseurService;
        this.currentUserService = currentUserService;
        this.auditEventPublisher = auditEventPublisher;
    }

    /** Retourne les lots actifs des produits donnés dans un magasin (utilisé par la recherche produit vendeur). */
    @Override
    public List<EntreeStock> findActiveLotsByMagasinAndProductIds(UUID magasinId, List<UUID> productIds) {
        return entreeStockDomainService.findActiveLotsByMagasinAndProductIds(magasinId, productIds);
    }

    /**
     * Résout magasin + fournisseur (scoping entreprise), matérialise chaque ligne (lot + stock + journal +
     * prixVente PF) puis publie un unique {@code AuditEvent STOCK_INITIAL_ENTRY} pour le batch.
     */
    @Override
    @Transactional
    public List<EntreeStockResponse> create(EntreeStockRequest entreeStockRequest) {
        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(entreeStockRequest.magasinId()));
        Fournisseur fournisseur = fournisseurService.ensureBelongsToCurrentEntreprise(fournisseurService.findById(entreeStockRequest.fournisseurId()));

        List<EntreeStockResponse> responses = entreeStockRequest.lignes().stream()
                .map(ligne -> materializeLine(magasin, fournisseur, ligne))
                .toList();

        publishAuditEvent(magasin);

        return responses;
    }

    /**
     * Matérialise une ligne : valide la marge, findOrCreate le PF, crée le lot {@code EntreeStock}
     * (commandeAchat null), upsert le {@code Stock} agrégé (PMP), journalise {@code ENTREE_INITIAL}
     * et met à jour le prixVente du PF (mêmes étapes que {@code AchatServiceImpl.materializeStockForLigne}).
     */
    public EntreeStockResponse materializeLine(Magasin magasin, Fournisseur fournisseur, LigneEntreeStockRequest ligne) {
        productFournisseurService.ensurePrixVenteGreaterThanPrixAchat(ligne.prixVente(), ligne.prixAchat());

        ProductFournisseur productFournisseur = resolveProductFournisseur(fournisseur, ligne);
        Product produit = productFournisseur.getProduct();

        int stockAvant = stockDomainService.findByMagasinIdAndProductFournisseurId(magasin.getId(), productFournisseur.getId())
                .map(Stock::getQuantiteDisponible)
                .orElse(0);

        EntreeStock entreeStock = entreeStockDomainService.create(new EntreeStockCreate(
                magasin, produit, productFournisseur,
                ligne.quantite(), ligne.prixAchat(),
                ligne.numeroLot(), ligne.dateExpiration(),
                null));

        Stock stock = stockDomainService.createOrUpdateEntry(
                new StockEntryContext(magasin, productFournisseur, ligne.quantite(), ligne.prixAchat()));

        mouvementStockDomainService.journalize(stock, new MouvementJournalize(
                MouvementStockType.ENTREE_INITIAL,
                ligne.quantite(),
                stockAvant,
                stock.getQuantiteDisponible(),
                ligne.numeroLot(),
                null));

        productFournisseurService.applyPrixVenteFromPurchase(productFournisseur, ligne.prixVente());

        return new EntreeStockResponse(entreeStock);
    }

    /** findOrCreate du ProductFournisseur (productId, fournisseurId, qualityId) puis re-fetch entité pour usage downstream. */
    public ProductFournisseur resolveProductFournisseur(Fournisseur fournisseur, LigneEntreeStockRequest ligne) {
        UUID pfId = productFournisseurService.findOrCreate(new ProductFournisseurRequest(
                ligne.productId(), fournisseur.getId(), ligne.qualityId(),
                ligne.prixAchat(), ligne.prixVente(),
                null, null
        )).id();
        return productFournisseurService.findById(pfId);
    }

    /** Publie un AuditEvent STOCK_INITIAL_ENTRY global (entityId null, label "ENTREE INITIAL"). */
    public void publishAuditEvent(Magasin magasin) {
        UserPrincipal caller = currentUserService.getCurrent();
        auditEventPublisher.publish(new AuditEvent(
                AuditAction.STOCK_INITIAL_ENTRY, AuditEntityType.STOCK,
                null, "ENTREE INITIAL",
                caller.accountId().toString(), caller.username(), caller.entrepriseId(),
                magasin.getId(), null));
    }
}
