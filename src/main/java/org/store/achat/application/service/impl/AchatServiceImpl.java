package org.store.achat.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.dto.AchatDetailsResponse;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.achat.application.dto.AchatDraftResponse;
import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatReceiveRequest;
import org.store.achat.application.dto.AchatResponse;
import org.store.achat.application.dto.AnnulationAchatRequest;
import org.store.achat.application.dto.AnnulationAchatResponse;
import org.store.achat.application.dto.CommandeAchatCreate;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.application.dto.FactureAchatCreate;
import org.store.achat.application.dto.FactureAchatCreateRequest;
import org.store.achat.application.dto.FactureAchatResponse;
import org.store.achat.application.dto.LigneAchatRequest;
import org.store.achat.application.dto.LigneAchatUpdateRequest;
import org.store.achat.application.dto.LigneCommandeAchatCreate;
import org.store.achat.application.dto.LigneCommandeAchatResponse;
import org.store.achat.application.dto.LigneCommandeAchatUpdate;
import org.store.achat.application.dto.RetraitStockResult;
import org.store.achat.application.service.IAchatService;
import org.store.achat.application.service.ICommandeAchatService;
import org.store.achat.application.service.IFournisseurService;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.model.FactureAchat;
import org.store.achat.domain.model.Fournisseur;
import org.store.achat.domain.model.LigneCommandeAchat;
import org.store.achat.application.dto.PaiementAchatCreate;
import org.store.achat.application.dto.PaiementAchatRequest;
import org.store.achat.domain.service.CommandeAchatDomainService;
import org.store.achat.domain.service.FactureAchatDomainService;
import org.store.achat.domain.service.LigneCommandeAchatDomainService;
import org.store.achat.domain.service.PaiementAchatDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.property.PurchaseProperties;
import org.store.stock.application.dto.EntreeStockCreate;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.StockEntryContext;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.EntreeStockDomainService;
import org.store.stock.domain.service.MouvementStockDomainService;
import org.store.stock.domain.service.StockDomainService;
import org.store.paiement.application.service.IMoyenPaiementService;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestre le cycle achat en 2 étapes :
 * <ol>
 *     <li>Création DRAFT (commande + lignes) — visible et éditable avant engagement.</li>
 *     <li>Réception (matérialisation facture + paiement initial éventuel + entrées stock +
 *         journal ENTREE_ACHAT + maj prixVente PF + bascule RECEPTIONNEE).</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class AchatServiceImpl implements IAchatService {

    private final CommandeAchatDomainService commandeAchatDomainService;
    private final LigneCommandeAchatDomainService ligneCommandeAchatDomainService;
    private final FactureAchatDomainService factureAchatDomainService;
    private final PaiementAchatDomainService paiementAchatDomainService;
    private final EntreeStockDomainService entreeStockDomainService;
    private final StockDomainService stockDomainService;
    private final MouvementStockDomainService mouvementStockDomainService;
    private final IMagasinService magasinService;
    private final IFournisseurService fournisseurService;
    private final IProductFournisseurService productFournisseurService;
    private final ICommandeAchatService commandeAchatService;
    private final ValidatorService validatorService;
    private final PurchaseProperties purchaseProperties;
    private final ICurrentUserService currentUserService;
    private final IAuditEventPublisher auditEventPublisher;
    private final IMoyenPaiementService moyenPaiementService;

    public AchatServiceImpl(CommandeAchatDomainService commandeAchatDomainService,
                            LigneCommandeAchatDomainService ligneCommandeAchatDomainService,
                            FactureAchatDomainService factureAchatDomainService,
                            PaiementAchatDomainService paiementAchatDomainService,
                            EntreeStockDomainService entreeStockDomainService,
                            StockDomainService stockDomainService,
                            MouvementStockDomainService mouvementStockDomainService,
                            IMagasinService magasinService,
                            IFournisseurService fournisseurService,
                            IProductFournisseurService productFournisseurService,
                            ICommandeAchatService commandeAchatService,
                            ValidatorService validatorService,
                            PurchaseProperties purchaseProperties,
                            ICurrentUserService currentUserService,
                            IAuditEventPublisher auditEventPublisher,
                            IMoyenPaiementService moyenPaiementService) {
        this.commandeAchatDomainService = commandeAchatDomainService;
        this.ligneCommandeAchatDomainService = ligneCommandeAchatDomainService;
        this.factureAchatDomainService = factureAchatDomainService;
        this.paiementAchatDomainService = paiementAchatDomainService;
        this.entreeStockDomainService = entreeStockDomainService;
        this.stockDomainService = stockDomainService;
        this.mouvementStockDomainService = mouvementStockDomainService;
        this.magasinService = magasinService;
        this.fournisseurService = fournisseurService;
        this.productFournisseurService = productFournisseurService;
        this.commandeAchatService = commandeAchatService;
        this.validatorService = validatorService;
        this.purchaseProperties = purchaseProperties;
        this.currentUserService = currentUserService;
        this.auditEventPublisher = auditEventPublisher;
        this.moyenPaiementService = moyenPaiementService;
    }

    /** Crée la commande DRAFT et ses lignes (validations PF + prix), sans toucher au stock ni à la facture. */
    @Override
    @Transactional
    public AchatDraftResponse create(AchatRequest achatRequest) {
        validatorService.validate(achatRequest);

        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(achatRequest.magasinId()));
        Fournisseur fournisseur = fournisseurService.ensureBelongsToCurrentEntreprise(fournisseurService.findById(achatRequest.fournisseurId()));

        List<ProductFournisseur> productFournisseurs = resolveAndValidateProductFournisseurs(achatRequest, fournisseur);

        CommandeAchat commande = commandeAchatDomainService.create(new CommandeAchatCreate(
                fournisseur, magasin, achatRequest.dateCommande(),
                commandeAchatDomainService.generateReference(),
                CommandeAchatStatut.DRAFT
        ));

        List<LigneCommandeAchat> savedLignes = persistLignes(achatRequest, commande, productFournisseurs);

        BigDecimal montantTotal = achatRequest.lignes().stream()
                .map(l -> l.prixAchat().multiply(BigDecimal.valueOf(l.quantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        commandeAchatDomainService.updateMontantTotal(commande, montantTotal);

        List<LigneCommandeAchatResponse> lignesResponse = savedLignes.stream()
                .map(LigneCommandeAchatResponse::new)
                .toList();

        return new AchatDraftResponse(
                new CommandeAchatResponse(commandeAchatDomainService.findById(commande.getId())),
                lignesResponse
        );
    }

    /** Retourne les lignes d'une commande paginées — scoping entreprise du caller. */
    @Override
    public Page<LigneCommandeAchatResponse> findLignesByCommandeId(UUID commandeId, int page, int size) {
        commandeAchatService.ensureBelongsToCurrentEntreprise(commandeAchatService.findById(commandeId));
        return ligneCommandeAchatDomainService.findPagedByCommandeId(commandeId, page, size)
                .map(LigneCommandeAchatResponse::new);
    }

    /** Ajoute une ligne à une commande DRAFT existante (scoping PF + prix + delta montantTotal). */
    @Override
    @Transactional
    public LigneCommandeAchatResponse addLigne(UUID commandeId, LigneAchatRequest ligneAchatRequest) {
        validatorService.validate(ligneAchatRequest);

        CommandeAchat commande = commandeAchatService.ensureBelongsToCurrentEntreprise(commandeAchatService.findById(commandeId));
        ensureCommandeIsDraft(commande);

        Fournisseur fournisseur = commande.getFournisseur(); // null for anonymous purchases
        ProductFournisseur productFournisseur = resolveAndValidateLine(ligneAchatRequest, fournisseur);

        LigneCommandeAchat ligne = ligneCommandeAchatDomainService.create(new LigneCommandeAchatCreate(
                commande, productFournisseur,
                ligneAchatRequest.quantite(), ligneAchatRequest.prixAchat(), ligneAchatRequest.prixVente(),
                ligneAchatRequest.numeroLot(), ligneAchatRequest.dateExpiration()
        ));

        BigDecimal lineTotal = ligneAchatRequest.prixAchat().multiply(BigDecimal.valueOf(ligneAchatRequest.quantite()));
        commandeAchatDomainService.updateMontantTotal(commande, commande.getMontantTotal().add(lineTotal));

        return new LigneCommandeAchatResponse(ligne);
    }

    /**
     * Réceptionne une commande DRAFT en une seule transaction : crée la facture (montantTotal recalculé
     * depuis les lignes courantes) + applique le paiement initial éventuel, puis matérialise le stock pour
     * chaque ligne (création EntreeStock, upsert Stock agrégé, journal ENTREE_ACHAT, maj prixVente PF, maj
     * quantiteRecue). Bascule la commande en RECEPTIONNEE.
     */
    @Override
    @Transactional
    public AchatResponse receive(UUID commandeId, AchatReceiveRequest achatReceiveRequest) {
        validatorService.validate(achatReceiveRequest);

        CommandeAchat commande = commandeAchatService.ensureBelongsToCurrentEntreprise(commandeAchatService.findById(commandeId));
        ensureCommandeIsDraft(commande);

        BigDecimal montantTotal = commande.getLignes().stream()
                .map(ligne -> ligne.getPrixAchat().multiply(BigDecimal.valueOf(ligne.getQuantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FactureAchatCreateRequest factureRequest = achatReceiveRequest.facture();
        String numero = resolveNumeroFacture(factureRequest.numero());

        FactureAchat facture = factureAchatDomainService.create(new FactureAchatCreate(
                commande, numero,
                factureRequest.date(), factureRequest.dateEcheance(),
                montantTotal
        ));

        FactureAchat factureAfterPaiement = applyOptionalInitialPaiement(facture, montantTotal, achatReceiveRequest.paiement());

        commande.getLignes().forEach(ligne -> materializeStockForLigne(commande, factureAfterPaiement, ligne));

        CommandeAchat receptionnee = commandeAchatDomainService.markReceptionnee(commande);

        return new AchatResponse(
                new CommandeAchatResponse(receptionnee),
                new FactureAchatResponse(factureAfterPaiement)
        );
    }

    /**
     * Si l'OWNER a renseigné un paiement initial dans le payload de
     * validate, on le persiste dans la même transaction. Refuse
     * l'overpaiement (montant > total) avec un message i18n
     * `paiementAchat.montant.exceedsRemaining`. Retourne la facture
     * post-paiement (statut + montantPaye mis à jour) — ou la facture
     * inchangée si aucun paiement n'a été fourni.
     */
    @Override
    public FactureAchat applyOptionalInitialPaiement(FactureAchat facture, BigDecimal montantTotal, PaiementAchatRequest paiement) {
        if (paiement == null) return facture;

        if (paiement.montant().compareTo(montantTotal) > 0) {
            throw new BadArgumentException("paiementAchat.montant.exceedsRemaining", montantTotal);
        }

        paiementAchatDomainService.create(new PaiementAchatCreate(
                facture, paiement.montant(), paiement.datePaiement(),
                moyenPaiementService.findById(paiement.moyenPaiementId())
        ));
        return factureAchatDomainService.applyPaiement(facture, paiement.montant());
    }

    /**
     * Résout le numéro de facture : si l'OWNER l'a fourni, vérifie son
     * unicité (sinon `UniqueResourceException` + message i18n
     * `factureAchat.numero.alreadyExists`) ; sinon, en génère un au
     * format `FACT-yyyyMMdd-HHmmssSSS`.
     */
    @Override
    public String resolveNumeroFacture(String numero) {
        if (numero == null || numero.isBlank()) {
            return factureAchatDomainService.generateNumero();
        }

        String trimmed = numero.trim();
        if (factureAchatDomainService.existsByNumero(trimmed)) {
            throw new UniqueResourceException("factureAchat.numero.alreadyExists", trimmed);
        }
        return trimmed;
    }

    /**
     * Matérialise le stock pour une ligne validée : crée l'EntreeStock (snapshot lot/expiration de la ligne),
     * upsert le Stock agrégé, journalise un ENTREE_ACHAT (ref facture), met à jour le prixVente PF et
     * incrémente quantiteRecue sur la ligne.
     */
    public void materializeStockForLigne(CommandeAchat commande, FactureAchat facture, LigneCommandeAchat ligne) {
        Magasin magasin = commande.getMagasin();
        ProductFournisseur productFournisseur = ligne.getProductFournisseur();
        Product produit = productFournisseur.getProduct();
        int quantite = ligne.getQuantite();

        int stockAvant = stockDomainService.findByMagasinIdAndProductFournisseurId(magasin.getId(), productFournisseur.getId())
                .map(Stock::getQuantiteDisponible).orElse(0);

        entreeStockDomainService.create(new EntreeStockCreate(
                magasin, produit, productFournisseur,
                quantite, ligne.getPrixAchat(),
                ligne.getNumeroLot(), ligne.getDateExpiration(),
                commande
        ));

        Stock stock = stockDomainService.createOrUpdateEntry(new StockEntryContext(magasin, productFournisseur, quantite, ligne.getPrixAchat()));

        mouvementStockDomainService.journalize(stock, new MouvementJournalize(
                MouvementStockType.ENTREE_ACHAT,
                quantite, stockAvant, stock.getQuantiteDisponible(),
                commande.getReference(),
                null
        ));

        productFournisseurService.applyPrixVenteFromPurchase(productFournisseur, ligne.getPrixVente());
    }

    /** Édite une ligne d'une commande DRAFT (quantité, prix, traçabilité lot) après validations publiques. */
    @Override
    @Transactional
    public LigneCommandeAchatResponse updateLigne(UUID commandeId, UUID ligneId, LigneAchatUpdateRequest ligneAchatUpdateRequest) {
        validatorService.validate(ligneAchatUpdateRequest);

        CommandeAchat commande = commandeAchatService.ensureBelongsToCurrentEntreprise(commandeAchatService.findById(commandeId));
        ensureCommandeIsDraft(commande);

        LigneCommandeAchat ligne = ensureLigneBelongsToCommande(ligneCommandeAchatDomainService.findById(ligneId), commande);
        productFournisseurService.ensurePrixVenteGreaterThanPrixAchat(ligneAchatUpdateRequest.prixVente(), ligneAchatUpdateRequest.prixAchat());

        BigDecimal oldLineTotal = ligne.getPrixAchat().multiply(BigDecimal.valueOf(ligne.getQuantite()));
        BigDecimal newLineTotal = ligneAchatUpdateRequest.prixAchat().multiply(BigDecimal.valueOf(ligneAchatUpdateRequest.quantite()));

        LigneCommandeAchat updated = ligneCommandeAchatDomainService.update(
                ligne,
                new LigneCommandeAchatUpdate(
                        ligneAchatUpdateRequest.quantite(),
                        ligneAchatUpdateRequest.prixAchat(),
                        ligneAchatUpdateRequest.prixVente(),
                        ligneAchatUpdateRequest.numeroLot(),
                        ligneAchatUpdateRequest.dateExpiration()
                )
        );

        BigDecimal updatedTotal = commande.getMontantTotal().subtract(oldLineTotal).add(newLineTotal);
        commandeAchatDomainService.updateMontantTotal(commande, updatedTotal);

        return new LigneCommandeAchatResponse(updated);
    }

    /** Supprime une ligne d'une commande DRAFT après validations (commande DRAFT + ligne in commande + pas la dernière). */
    @Override
    @Transactional
    public void deleteLigne(UUID commandeId, UUID ligneId) {
        CommandeAchat commande = commandeAchatService.ensureBelongsToCurrentEntreprise(commandeAchatService.findById(commandeId));
        ensureCommandeIsDraft(commande);

        LigneCommandeAchat ligne = ensureLigneBelongsToCommande(ligneCommandeAchatDomainService.findById(ligneId), commande);
        ensureNotLastLigne(commande);

        BigDecimal lineTotal = ligne.getPrixAchat().multiply(BigDecimal.valueOf(ligne.getQuantite()));
        ligneCommandeAchatDomainService.delete(ligne);
        commandeAchatDomainService.updateMontantTotal(commande, commande.getMontantTotal().subtract(lineTotal));
    }

    /**
     * Supprime une commande encore en DRAFT (abandon de brouillon). Les lignes sont supprimées en cascade
     * (mapping `@OneToMany(cascade = ALL)` côté entité). Aucune facture/stock/paiement à défaire — le statut
     * DRAFT garantit l'absence de matérialisation. Refuse si la commande n'est plus en DRAFT.
     */
    @Override
    @Transactional
    public void deleteDraft(UUID commandeId) {
        CommandeAchat commande = commandeAchatService.ensureBelongsToCurrentEntreprise(commandeAchatService.findById(commandeId));
        ensureCommandeIsDraft(commande);

        commandeAchatDomainService.delete(commande);
    }

    /** Retourne le détail d'un achat : commande + facture (null si DRAFT) + lignes. Scoping entreprise via la commande. */
    @Override
    public AchatDetailsResponse findDetailsById(UUID commandeId) {
        CommandeAchat commande = commandeAchatService.ensureBelongsToCurrentEntreprise(commandeAchatService.findById(commandeId));

        Optional<FactureAchat> facture = factureAchatDomainService.findByCommandeId(commande.getId());

        List<LigneCommandeAchatResponse> lignes = commande.getLignes().stream()
                .map(LigneCommandeAchatResponse::new)
                .toList();

        return new AchatDetailsResponse(
                new CommandeAchatResponse(commande),
                facture.map(FactureAchatResponse::new).orElse(null),
                lignes
        );
    }

    /** Lève BadArgument si la commande n'est pas en DRAFT (déjà validée ou réceptionnée). */
    public void ensureCommandeIsDraft(CommandeAchat commande) {
        if (commande.getStatut() != CommandeAchatStatut.DRAFT) {
            throw new BadArgumentException("commandeAchat.notDraft", commande.getStatut().name());
        }
    }

    /** Lève BadArgument si la ligne n'appartient pas à la commande ciblée (anti URL forgée). */
    public LigneCommandeAchat ensureLigneBelongsToCommande(LigneCommandeAchat ligne, CommandeAchat commande) {
        if (ligne.getCommande() == null || !ligne.getCommande().getId().equals(commande.getId())) {
            throw new BadArgumentException("ligneCommandeAchat.notMatchingCommande");
        }
        return ligne;
    }

    /** Lève BadArgument si la commande n'a qu'une seule ligne (suppression interdite, commande vide non autorisée). */
    public void ensureNotLastLigne(CommandeAchat commande) {
        if (commande.getLignes() == null || commande.getLignes().size() <= 1) {
            throw new BadArgumentException("commandeAchat.cannotDeleteLastLigne");
        }
    }

    /** Résout chaque productFournisseur, vérifie scoping + cohérence fournisseur + valide prixVente > prixAchat. */
    public List<ProductFournisseur> resolveAndValidateProductFournisseurs(AchatRequest request, Fournisseur fournisseur) {
        return request.lignes().stream()
                .map(ligne -> resolveAndValidateLine(ligne, fournisseur))
                .toList();
    }

    /** Valide prixVente > prixAchat puis trouve ou crée le ProductFournisseur. */
    public ProductFournisseur resolveAndValidateLine(LigneAchatRequest ligne, Fournisseur fournisseur) {
        productFournisseurService.ensurePrixVenteGreaterThanPrixAchat(ligne.prixVente(), ligne.prixAchat());

        org.store.produit.application.dto.ProductFournisseurResponse pf = productFournisseurService.findOrCreate(
                new org.store.produit.application.dto.ProductFournisseurRequest(
                        ligne.productId(), fournisseur.getId(), ligne.qualityId(),
                        ligne.prixAchat(), ligne.prixVente(), null, null
                )
        );
        return productFournisseurService.findById(pf.id());
    }

    /** Persiste chaque ligne de la commande DRAFT (snapshot prix + traçabilité lot) et retourne les entités sauvegardées. */
    public List<LigneCommandeAchat> persistLignes(AchatRequest request, CommandeAchat commande, List<ProductFournisseur> productFournisseurs) {
        List<LigneAchatRequest> lignes = request.lignes();
        Iterator<ProductFournisseur> productFournisseurIterator = productFournisseurs.iterator();

        return lignes.stream()
                .map(ligne -> ligneCommandeAchatDomainService.create(new LigneCommandeAchatCreate(
                        commande, productFournisseurIterator.next(),
                        ligne.quantite(), ligne.prixAchat(), ligne.prixVente(),
                        ligne.numeroLot(), ligne.dateExpiration()
                )))
                .toList();
    }

    /**
     * Annule une commande RECEPTIONNEE dans la fenêtre autorisée, retire le stock alimenté par cet achat
     * (chaque lot doit être intact : aucun lot consommé par une vente), bascule commande + facture en ANNULEE.
     */
    @Override
    @Transactional
    public AnnulationAchatResponse cancel(UUID commandeId, AnnulationAchatRequest annulationAchatRequest) {
        validatorService.validate(annulationAchatRequest);

        CommandeAchat commande = commandeAchatService.ensureBelongsToCurrentEntreprise(commandeAchatService.findById(commandeId));
        ensureCancellable(commande);
        ensureWithinCancelWindow(commande);
        ensureNoPaiementRecorded(commande);

        List<EntreeStock> lots = entreeStockDomainService.findByCommandeAchatId(commande.getId());
        ensureNoLotConsumed(lots);

        RetraitStockResult retrait = lots.stream()
                .map(lot -> withdrawStockForLot(commande, lot))
                .reduce(RetraitStockResult.empty(), RetraitStockResult::merge);

        CommandeAchat cancelled = commandeAchatDomainService.cancel(commande, annulationAchatRequest.motifAsEnum(), annulationAchatRequest.commentaire());

        Optional<FactureAchat> facture = factureAchatDomainService.findByCommandeId(cancelled.getId());
        facture.ifPresent(factureAchatDomainService::cancel);

        UserPrincipal caller = currentUserService.getCurrent();
        auditEventPublisher.publish(new AuditEvent(
                AuditAction.ACHAT_CANCELLED, AuditEntityType.COMMANDE_ACHAT,
                cancelled.getId(), cancelled.getReference(),
                caller.accountId().toString(), caller.username(), caller.entrepriseId(),
                cancelled.getMagasin().getId(), null));

        return new AnnulationAchatResponse(cancelled, retrait.totalQuantite(), retrait.nombreMouvements());
    }

    /**
     * Refuse l'annulation d'une commande dont la facture associée a
     * déjà reçu un paiement (`montantPaye > 0`). Cancel reverse les
     * mouvements stock mais ne touche pas aux paiements — accepter
     * l'annulation laisserait un paiement orphelin contre une facture
     * basculée en ANNULEE. Le workflow correct est d'enregistrer un
     * remboursement (hors-scope V1), pas d'annuler.
     */
    public void ensureNoPaiementRecorded(CommandeAchat commande) {
        Optional<FactureAchat> facture = factureAchatDomainService.findByCommandeId(commande.getId());
        facture.ifPresent(f -> {
            BigDecimal montantPaye = f.getMontantPaye() != null ? f.getMontantPaye() : BigDecimal.ZERO;
            if (montantPaye.compareTo(BigDecimal.ZERO) > 0) {
                throw new BadArgumentException("commandeAchat.cancel.hasPaiements");
            }
        });
    }

    /**
     * Lève BadArgument si la commande n'est pas dans un statut annulable (RECEPTIONNEE uniquement) —
     * DRAFT exclu (rien à annuler, supprimer les lignes pour abandonner un brouillon) et ANNULEE exclu
     * (déjà annulée).
     */
    public void ensureCancellable(CommandeAchat commande) {
        CommandeAchatStatut statut = commande.getStatut();
        if (statut == CommandeAchatStatut.ANNULEE) {
            throw new BadArgumentException("commandeAchat.cancel.alreadyCancelled");
        }
        if (statut != CommandeAchatStatut.RECEPTIONNEE) {
            throw new BadArgumentException("commandeAchat.cancel.notCancellable", statut.name());
        }
    }

    /** Lève BadArgument si la commande dépasse la fenêtre d'annulation autorisée (configurable via PurchaseProperties). */
    public void ensureWithinCancelWindow(CommandeAchat commande) {
        int maxHours = purchaseProperties.cancelWindowHours();
        LocalDateTime deadline = commande.getCreatedAt().plusHours(maxHours);
        if (deadline.isBefore(LocalDateTime.now())) {
            throw new BadArgumentException("commandeAchat.cancel.windowExpired", maxHours);
        }
    }

    /**
     * Lève BadArgument si au moins un lot a été partiellement ou totalement consommé par une vente
     * ({@code quantiteRestante < quantiteInitiale}) — l'annulation symétrique n'est plus possible dans ce cas.
     */
    public void ensureNoLotConsumed(List<EntreeStock> lots) {
        boolean atLeastOneConsumed = lots.stream()
                .anyMatch(lot -> lot.getQuantiteRestante() < lot.getQuantiteInitiale());
        if (atLeastOneConsumed) {
            throw new BadArgumentException("commandeAchat.cancel.lotAlreadyConsumed");
        }
    }

    /** Décrémente le stock agrégé de la quantité du lot, marque le lot comme annulé et journalise un RETOUR_FOURNISSEUR. */
    public RetraitStockResult withdrawStockForLot(CommandeAchat commande, EntreeStock lot) {
        int quantite = lot.getQuantiteRestante();

        Stock stock = stockDomainService.findByMagasinIdAndProductFournisseurId(lot.getMagasin().getId(), lot.getProductFournisseur().getId())
                .orElseThrow(() -> new EntityException("stock.notFound"));
        int stockAvant = stock.getQuantiteDisponible();

        Stock updated = stockDomainService.decrement(stock, quantite);
        entreeStockDomainService.markAsAnnulee(lot);

        mouvementStockDomainService.journalize(updated, new MouvementJournalize(
                MouvementStockType.RETOUR_FOURNISSEUR,
                quantite,
                stockAvant,
                updated.getQuantiteDisponible(),
                commande.getReference(),
                null
        ));

        return new RetraitStockResult(quantite, 1);
    }
}
