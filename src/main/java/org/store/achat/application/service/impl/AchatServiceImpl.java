package org.store.achat.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.dto.AchatDetailsResponse;
import org.store.achat.application.dto.AchatDraftResponse;
import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatResponse;
import org.store.achat.application.dto.AchatValidateRequest;
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
import org.store.achat.application.dto.RetraitStockResult;
import org.store.achat.application.service.IAchatService;
import org.store.achat.application.service.ICommandeAchatService;
import org.store.achat.application.service.IFournisseurService;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.model.FactureAchat;
import org.store.achat.domain.model.Fournisseur;
import org.store.achat.domain.model.LigneCommandeAchat;
import org.store.achat.domain.service.CommandeAchatDomainService;
import org.store.achat.domain.service.FactureAchatDomainService;
import org.store.achat.domain.service.LigneCommandeAchatDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.property.PurchaseProperties;
import org.store.stock.application.dto.EntreeStockCreate;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.EntreeStockDomainService;
import org.store.stock.domain.service.MouvementStockDomainService;
import org.store.stock.domain.service.StockDomainService;

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
 *     <li>Validation (matérialisation facture + entrées stock + journal + update prixVente PF + bascule RECEPTIONNEE).</li>
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class AchatServiceImpl implements IAchatService {

    private final CommandeAchatDomainService commandeAchatDomainService;
    private final LigneCommandeAchatDomainService ligneCommandeAchatDomainService;
    private final FactureAchatDomainService factureAchatDomainService;
    private final EntreeStockDomainService entreeStockDomainService;
    private final StockDomainService stockDomainService;
    private final MouvementStockDomainService mouvementStockDomainService;
    private final IMagasinService magasinService;
    private final IFournisseurService fournisseurService;
    private final IProductFournisseurService productFournisseurService;
    private final ICommandeAchatService commandeAchatService;
    private final ValidatorService validatorService;
    private final PurchaseProperties purchaseProperties;

    public AchatServiceImpl(CommandeAchatDomainService commandeAchatDomainService,
                            LigneCommandeAchatDomainService ligneCommandeAchatDomainService,
                            FactureAchatDomainService factureAchatDomainService,
                            EntreeStockDomainService entreeStockDomainService,
                            StockDomainService stockDomainService,
                            MouvementStockDomainService mouvementStockDomainService,
                            IMagasinService magasinService,
                            IFournisseurService fournisseurService,
                            IProductFournisseurService productFournisseurService,
                            ICommandeAchatService commandeAchatService,
                            ValidatorService validatorService,
                            PurchaseProperties purchaseProperties) {
        this.commandeAchatDomainService = commandeAchatDomainService;
        this.ligneCommandeAchatDomainService = ligneCommandeAchatDomainService;
        this.factureAchatDomainService = factureAchatDomainService;
        this.entreeStockDomainService = entreeStockDomainService;
        this.stockDomainService = stockDomainService;
        this.mouvementStockDomainService = mouvementStockDomainService;
        this.magasinService = magasinService;
        this.fournisseurService = fournisseurService;
        this.productFournisseurService = productFournisseurService;
        this.commandeAchatService = commandeAchatService;
        this.validatorService = validatorService;
        this.purchaseProperties = purchaseProperties;
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

        persistLignes(achatRequest, commande, productFournisseurs);

        return new AchatDraftResponse(new CommandeAchatResponse(commandeAchatDomainService.findById(commande.getId())));
    }

    /** Matérialise une commande DRAFT : facture + entrées stock + journal + update prixVente PF + bascule RECEPTIONNEE. */
    @Override
    @Transactional
    public AchatResponse validate(UUID commandeId, AchatValidateRequest achatValidateRequest) {
        validatorService.validate(achatValidateRequest);

        CommandeAchat commande = commandeAchatService.ensureBelongsToCurrentEntreprise(commandeAchatService.findById(commandeId));
        ensureCommandeIsDraft(commande);

        List<LigneCommandeAchat> lignes = commande.getLignes();
        Magasin magasin = commande.getMagasin();

        BigDecimal montantTotal = lignes.stream()
                .map(ligne -> ligne.getPrixAchat().multiply(BigDecimal.valueOf(ligne.getQuantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FactureAchatCreateRequest factureRequest = achatValidateRequest.facture();
        FactureAchat facture = factureAchatDomainService.create(new FactureAchatCreate(
                commande, factureRequest.numero(),
                factureRequest.date(), factureRequest.dateEcheance(),
                montantTotal
        ));

        lignes.forEach(ligne -> materializeLigneStock(magasin, commande, facture, ligne));

        CommandeAchat receptionnee = commandeAchatDomainService.validate(commande);

        return new AchatResponse(
                new CommandeAchatResponse(receptionnee),
                new FactureAchatResponse(facture)
        );
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

        LigneCommandeAchat updated = ligneCommandeAchatDomainService.update(
                ligne,
                ligneAchatUpdateRequest.quantite(),
                ligneAchatUpdateRequest.prixAchat(),
                ligneAchatUpdateRequest.prixVente(),
                ligneAchatUpdateRequest.numeroLot(),
                ligneAchatUpdateRequest.dateExpiration()
        );

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

        ligneCommandeAchatDomainService.delete(ligne);
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

    /** Valide prixVente > prixAchat, résout le PF, vérifie son scoping entreprise et sa cohérence avec le fournisseur cible. */
    public ProductFournisseur resolveAndValidateLine(LigneAchatRequest ligne, Fournisseur fournisseur) {
        productFournisseurService.ensurePrixVenteGreaterThanPrixAchat(ligne.prixVente(), ligne.prixAchat());

        ProductFournisseur productFournisseur = productFournisseurService.ensureBelongsToCurrentEntreprise(
                productFournisseurService.findById(ligne.productFournisseurId()));

        if (!productFournisseur.getFournisseur().getId().equals(fournisseur.getId())) {
            throw new BadArgumentException("achat.fournisseur.productMismatch");
        }
        return productFournisseur;
    }

    /** Persiste chaque ligne de la commande DRAFT (snapshot prix + traçabilité lot). */
    public void persistLignes(AchatRequest request, CommandeAchat commande, List<ProductFournisseur> productFournisseurs) {
        List<LigneAchatRequest> lignes = request.lignes();
        Iterator<ProductFournisseur> productFournisseurIterator = productFournisseurs.iterator();

        lignes.forEach(ligne -> ligneCommandeAchatDomainService.create(new LigneCommandeAchatCreate(
                commande, productFournisseurIterator.next(),
                ligne.quantite(), ligne.prixAchat(), ligne.prixVente(),
                ligne.numeroLot(), ligne.dateExpiration()
        )));
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

        List<EntreeStock> lots = entreeStockDomainService.findByCommandeAchatId(commande.getId());
        ensureNoLotConsumed(lots);

        RetraitStockResult retrait = lots.stream()
                .map(lot -> withdrawStockForLot(commande, lot))
                .reduce(RetraitStockResult.empty(), RetraitStockResult::merge);

        CommandeAchat cancelled = commandeAchatDomainService.cancel(commande, annulationAchatRequest.motifAsEnum(), annulationAchatRequest.commentaire());

        Optional<FactureAchat> facture = factureAchatDomainService.findByCommandeId(cancelled.getId());
        facture.ifPresent(factureAchatDomainService::cancel);

        return new AnnulationAchatResponse(cancelled, retrait.totalQuantite(), retrait.nombreMouvements());
    }

    /** Lève BadArgument si la commande n'est pas en statut RECEPTIONNEE (déjà annulée ou pas encore validée). */
    public void ensureCancellable(CommandeAchat commande) {
        CommandeAchatStatut statut = commande.getStatut();
        if (statut == CommandeAchatStatut.ANNULEE) {
            throw new BadArgumentException("commandeAchat.cancel.alreadyCancelled");
        }
        if (statut != CommandeAchatStatut.RECEPTIONNEE) {
            throw new BadArgumentException("commandeAchat.cancel.notReceptionnee", statut.name());
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

        Stock stock = stockDomainService.findByMagasinIdAndProduitId(lot.getMagasin().getId(), lot.getProduit().getId())
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

    /** Crée le lot d'entrée stock pour une ligne validée, upsert le Stock agrégé, journalise et met à jour le prixVente PF courant. */
    public void materializeLigneStock(Magasin magasin, CommandeAchat commande, FactureAchat facture, LigneCommandeAchat ligne) {
        ProductFournisseur productFournisseur = ligne.getProductFournisseur();
        Product produit = productFournisseur.getProduct();

        int stockAvant = stockDomainService.findByMagasinIdAndProduitId(magasin.getId(), produit.getId())
                .map(Stock::getQuantiteDisponible).orElse(0);

        entreeStockDomainService.create(new EntreeStockCreate(
                magasin, produit, productFournisseur,
                ligne.getQuantite(), ligne.getPrixAchat(),
                ligne.getNumeroLot(), ligne.getDateExpiration(),
                commande
        ));

        Stock stock = stockDomainService.createOrUpdateEntry(magasin, produit, ligne.getQuantite(), ligne.getPrixAchat());

        mouvementStockDomainService.journalize(stock, new MouvementJournalize(
                MouvementStockType.ENTREE_ACHAT,
                ligne.getQuantite(), stockAvant, stock.getQuantiteDisponible(),
                facture.getNumero(),
                null
        ));

        productFournisseurService.applyPrixVenteFromPurchase(productFournisseur, ligne.getPrixVente());
    }
}
