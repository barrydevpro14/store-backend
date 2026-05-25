package org.store.vente.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.dto.UserSummaryResponse;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.property.SaleProperties;
import org.store.common.tools.OwnershipHelper;
import org.store.security.application.service.IAccountService;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.SortieStockForVente;
import org.store.stock.application.service.ISortieStockService;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.SortieStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.EntreeStockDomainService;
import org.store.stock.domain.service.MouvementStockDomainService;
import org.store.stock.domain.service.SortieStockDomainService;
import org.store.stock.domain.service.StockDomainService;
import org.store.users.application.service.IEmployeService;
import org.store.users.domain.model.Employe;
import org.store.vente.application.dto.AnnulationVenteRequest;
import org.store.vente.application.dto.AnnulationVenteResponse;
import org.store.vente.application.dto.CommandeVenteCreate;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.application.dto.FactureClientCreate;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.application.dto.LigneCommandeVenteCreate;
import org.store.vente.application.dto.LigneCommandeVenteResponse;
import org.store.vente.application.dto.LigneVenteRequest;
import org.store.vente.application.dto.LigneVenteUpdateRequest;
import org.store.vente.application.dto.PaiementVenteCreate;
import org.store.vente.application.dto.PaiementVenteRequest;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.application.dto.ReinjectionStockResult;
import org.store.vente.application.dto.VenteDetailsResponse;
import org.store.vente.application.dto.VenteDraftResponse;
import org.store.vente.application.dto.VenteRequest;
import org.store.vente.application.dto.VenteResponse;
import org.store.vente.application.dto.VenteValidateRequest;
import org.store.vente.application.service.IClientService;
import org.store.vente.application.service.IVenteService;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.model.Client;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.model.LigneCommandeVente;
import org.store.vente.domain.model.PaiementVente;
import org.store.vente.domain.service.CommandeVenteDomainService;
import org.store.vente.domain.service.FactureClientDomainService;
import org.store.vente.domain.service.LigneCommandeVenteDomainService;
import org.store.vente.domain.service.PaiementVenteDomainService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestre le cycle vente en 2 étapes :
 * <ol>
 *     <li>Création DRAFT (commande + lignes, validations prix + scoping PF) — visible et éditable avant encaissement.</li>
 *     <li>Validation (consommation stock FIFO + facture + paiement initial éventuel + bascule VALIDATE).</li>
 * </ol>
 * Cancel : sur VALIDATE uniquement (workflow d'annulation = ré-injection stock + statut CANCEL).
 */
@Service
@Transactional(readOnly = true)
public class VenteServiceImpl implements IVenteService {

    private final CommandeVenteDomainService commandeVenteDomainService;
    private final LigneCommandeVenteDomainService ligneCommandeVenteDomainService;
    private final FactureClientDomainService factureClientDomainService;
    private final PaiementVenteDomainService paiementVenteDomainService;
    private final IEmployeService employeService;
    private final IClientService clientService;
    private final IProductFournisseurService productFournisseurService;
    private final ISortieStockService sortieStockService;
    private final IAccountService accountService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;
    private final EntreeStockDomainService entreeStockDomainService;
    private final SortieStockDomainService sortieStockDomainService;
    private final StockDomainService stockDomainService;
    private final MouvementStockDomainService mouvementStockDomainService;
    private final SaleProperties saleProperties;

    public VenteServiceImpl(CommandeVenteDomainService commandeVenteDomainService,
                            LigneCommandeVenteDomainService ligneCommandeVenteDomainService,
                            FactureClientDomainService factureClientDomainService,
                            PaiementVenteDomainService paiementVenteDomainService,
                            IEmployeService employeService,
                            IClientService clientService,
                            IProductFournisseurService productFournisseurService,
                            ISortieStockService sortieStockService,
                            IAccountService accountService,
                            ICurrentUserService currentUserService,
                            ValidatorService validatorService,
                            EntreeStockDomainService entreeStockDomainService,
                            SortieStockDomainService sortieStockDomainService,
                            StockDomainService stockDomainService,
                            MouvementStockDomainService mouvementStockDomainService,
                            SaleProperties saleProperties) {
        this.commandeVenteDomainService = commandeVenteDomainService;
        this.ligneCommandeVenteDomainService = ligneCommandeVenteDomainService;
        this.factureClientDomainService = factureClientDomainService;
        this.paiementVenteDomainService = paiementVenteDomainService;
        this.employeService = employeService;
        this.clientService = clientService;
        this.productFournisseurService = productFournisseurService;
        this.sortieStockService = sortieStockService;
        this.accountService = accountService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
        this.entreeStockDomainService = entreeStockDomainService;
        this.sortieStockDomainService = sortieStockDomainService;
        this.stockDomainService = stockDomainService;
        this.mouvementStockDomainService = mouvementStockDomainService;
        this.saleProperties = saleProperties;
    }

    /** Crée une commande de vente DRAFT + ses lignes (validations prix + scoping PF), sans toucher au stock. */
    @Override
    @Transactional
    public VenteDraftResponse create(VenteRequest venteRequest) {
        validatorService.validate(venteRequest);

        Employe user = employeService.findCurrentUser();
        Magasin magasin = user.getMagasin();
        Client client = resolveClient(venteRequest.clientId());
        LocalDate dateVente = LocalDate.now();

        List<ProductFournisseur> productFournisseurs = resolveAndValidateLignes(venteRequest);

        CommandeVente commande = commandeVenteDomainService.create(new CommandeVenteCreate(
                client, magasin, dateVente,
                commandeVenteDomainService.generateReference(),
                CommandeVenteStatut.DRAFT
        ));

        persistLignes(venteRequest, commande, productFournisseurs);

        CommandeVente refreshed = commandeVenteDomainService.findById(commande.getId());
        return new VenteDraftResponse(
                new CommandeVenteResponse(refreshed, BigDecimal.ZERO, BigDecimal.ZERO)
        );
    }

    /** Matérialise une commande DRAFT : consomme le stock FIFO par ligne, crée facture + paiement initial, bascule DELIVERED. */
    @Override
    @Transactional
    public VenteResponse validate(UUID commandeId, VenteValidateRequest venteValidateRequest) {
        validatorService.validate(venteValidateRequest);

        CommandeVente commande = ensureBelongsToCurrentEntreprise(commandeVenteDomainService.findById(commandeId));
        ensureCommandeIsDraft(commande);

        List<LigneCommandeVente> lignes = commande.getLignes();
        Magasin magasin = commande.getMagasin();

        lignes.forEach(ligne -> consumeStockForLigne(magasin, ligne));

        BigDecimal montantTotal = lignes.stream()
                .map(LigneCommandeVente::getMontantTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        FactureClient facture = factureClientDomainService.create(new FactureClientCreate(
                commande, factureClientDomainService.generateNumero(),
                commande.getDate(), venteValidateRequest.dateEcheance(), montantTotal
        ));

        FactureClient finalFacture = applyPremierPaiementIfPresent(venteValidateRequest.premierPaiement(), facture);

        CommandeVente delivered = commandeVenteDomainService.validate(commande);

        UserSummaryResponse userSummary = accountService.findUserSummaryByAccountId(delivered.getCreatedBy()).orElse(null);

        return new VenteResponse(
                new CommandeVenteResponse(delivered, userSummary, finalFacture),
                new FactureClientResponse(finalFacture)
        );
    }

    /** Édite une ligne d'une commande DRAFT (quantité, prixUnitaire) après validations publiques. */
    @Override
    @Transactional
    public LigneCommandeVenteResponse updateLigne(UUID commandeId, UUID ligneId, LigneVenteUpdateRequest ligneVenteUpdateRequest) {
        validatorService.validate(ligneVenteUpdateRequest);

        CommandeVente commande = ensureBelongsToCurrentEntreprise(commandeVenteDomainService.findById(commandeId));
        ensureCommandeIsDraft(commande);

        LigneCommandeVente ligne = ensureLigneBelongsToCommande(ligneCommandeVenteDomainService.findById(ligneId), commande);
        ensurePrixUnitaireAboveFloor(ligneVenteUpdateRequest.prixUnitaire(), ligne.getProductFournisseur());

        LigneCommandeVente updated = ligneCommandeVenteDomainService.update(ligne, ligneVenteUpdateRequest.quantite(), ligneVenteUpdateRequest.prixUnitaire());

        return new LigneCommandeVenteResponse(updated);
    }

    /** Supprime une ligne d'une commande DRAFT après validations (commande DRAFT + ligne in commande + pas la dernière). */
    @Override
    @Transactional
    public void deleteLigne(UUID commandeId, UUID ligneId) {
        CommandeVente commande = ensureBelongsToCurrentEntreprise(commandeVenteDomainService.findById(commandeId));
        ensureCommandeIsDraft(commande);

        LigneCommandeVente ligne = ensureLigneBelongsToCommande(ligneCommandeVenteDomainService.findById(ligneId), commande);
        ensureNotLastLigne(commande);

        ligneCommandeVenteDomainService.delete(ligne);
    }

    /** Retourne le détail d'une vente : commande + facture (null si DRAFT) + lignes + paiements (user résolu via createdBy). */
    @Override
    public VenteDetailsResponse findDetailsById(UUID commandeId) {
        CommandeVente commande = ensureBelongsToCurrentEntreprise(commandeVenteDomainService.findById(commandeId));

        Optional<FactureClient> facture = factureClientDomainService.findByCommandeId(commande.getId());

        List<PaiementVente> paiements = facture.map(f -> paiementVenteDomainService.findAllByFactureId(f.getId())).orElse(List.of());
        UserSummaryResponse user = accountService.findUserSummaryByAccountId(commande.getCreatedBy()).orElse(null);

        List<LigneCommandeVenteResponse> lignes = commande.getLignes().stream()
                .map(LigneCommandeVenteResponse::new)
                .toList();

        List<PaiementVenteResponse> paiementsResponse = paiements.stream()
                .map(PaiementVenteResponse::new)
                .toList();

        return new VenteDetailsResponse(
                new CommandeVenteResponse(commande, user, facture.orElse(null)),
                facture.map(FactureClientResponse::new).orElse(null),
                lignes,
                paiementsResponse
        );
    }

    /** Annule une vente DELIVERED dans la fenêtre autorisée, ré-injecte le stock et bascule commande + facture en ANNULEE. */
    @Override
    @Transactional
    public AnnulationVenteResponse cancel(UUID commandeId, AnnulationVenteRequest annulationVenteRequest) {
        validatorService.validate(annulationVenteRequest);

        CommandeVente commande = ensureBelongsToCurrentEntreprise(commandeVenteDomainService.findById(commandeId));
        ensureCancellable(commande);
        ensureWithinCancelWindow(commande);

        ReinjectionStockResult reinjection = commande.getLignes().stream()
                .map(this::reinjectStockForLigne)
                .reduce(ReinjectionStockResult.empty(), ReinjectionStockResult::merge);

        CommandeVente cancelled = commandeVenteDomainService.cancel(commande, annulationVenteRequest.motifAsEnum(), annulationVenteRequest.commentaire());

        Optional<FactureClient> facture = factureClientDomainService.findByCommandeId(cancelled.getId());
        facture.ifPresent(factureClientDomainService::cancel);

        return new AnnulationVenteResponse(cancelled, reinjection.totalQuantite(), reinjection.nombreMouvements());
    }

    /** Lève `ForbiddenException` si la commande n'appartient pas à l'entreprise du caller (via magasin.entreprise). */
    public CommandeVente ensureBelongsToCurrentEntreprise(CommandeVente commande) {
        return OwnershipHelper.ensureOwnership(
                commande,
                commande.getMagasin().getEntreprise().getId(),
                currentUserService.getCurrent().entrepriseId(),
                "commandeVente.notOwned"
        );
    }

    /** Lève BadArgument si la commande n'est pas en DRAFT (déjà validée ou annulée). */
    public void ensureCommandeIsDraft(CommandeVente commande) {
        if (commande.getStatut() != CommandeVenteStatut.DRAFT) {
            throw new BadArgumentException("commandeVente.notDraft", commande.getStatut().name());
        }
    }

    /** Lève BadArgument si la ligne n'appartient pas à la commande ciblée (anti URL forgée). */
    public LigneCommandeVente ensureLigneBelongsToCommande(LigneCommandeVente ligne, CommandeVente commande) {
        if (ligne.getCommande() == null || !ligne.getCommande().getId().equals(commande.getId())) {
            throw new BadArgumentException("ligneCommandeVente.notMatchingCommande");
        }
        return ligne;
    }

    /** Lève BadArgument si la commande n'a qu'une seule ligne (suppression interdite, commande vide non autorisée). */
    public void ensureNotLastLigne(CommandeVente commande) {
        if (commande.getLignes() == null || commande.getLignes().size() <= 1) {
            throw new BadArgumentException("commandeVente.cannotDeleteLastLigne");
        }
    }

    /** Vérifie {@code prixUnitaire ≥ pf.prixVente} (plancher fournisseur). Lève BadArgument sinon. */
    public void ensurePrixUnitaireAboveFloor(BigDecimal prixUnitaire, ProductFournisseur productFournisseur) {
        if (prixUnitaire.compareTo(productFournisseur.getPrixVente()) < 0) {
            throw new BadArgumentException("vente.prixUnitaire.belowFloor", productFournisseur.getPrixVente().toString());
        }
    }

    /** Résout un Client par id (avec scoping double employé/propriétaire) ou retourne null si aucun id fourni (vente anonyme). */
    public Client resolveClient(UUID clientId) {
        if (clientId == null) {
            return null;
        }
        return clientService.ensureAccessibleByCurrentUser(clientService.findById(clientId));
    }

    /** Résout chaque ProductFournisseur des lignes et valide prixUnitaire ≥ pf.prixVente. Throw BadArgument sinon. */
    public List<ProductFournisseur> resolveAndValidateLignes(VenteRequest request) {
        return request.lignes().stream()
                .map(this::resolveAndValidateLine)
                .toList();
    }

    /** Charge un ProductFournisseur scopé entreprise et valide que le prixUnitaire ne descend pas sous pf.prixVente. */
    public ProductFournisseur resolveAndValidateLine(LigneVenteRequest ligne) {
        ProductFournisseur productFournisseur = productFournisseurService.ensureBelongsToCurrentEntreprise(
                productFournisseurService.findById(ligne.productFournisseurId()));

        ensurePrixUnitaireAboveFloor(ligne.prixUnitaire(), productFournisseur);
        return productFournisseur;
    }

    /** Persiste chaque ligne de la commande DRAFT (snapshot prixUnitaire + montantTotal calculé). */
    public void persistLignes(VenteRequest request, CommandeVente commande, List<ProductFournisseur> productFournisseurs) {
        List<LigneVenteRequest> lignes = request.lignes();
        Iterator<ProductFournisseur> productFournisseurIterator = productFournisseurs.iterator();

        lignes.forEach(ligne -> ligneCommandeVenteDomainService.create(new LigneCommandeVenteCreate(
                commande, productFournisseurIterator.next(), ligne.quantite(), ligne.prixUnitaire()
        )));
    }

    /** Consomme les lots FIFO du PF d'une ligne validée (sorties stock + decrement Stock + journal SORTIE_VENTE). */
    public void consumeStockForLigne(Magasin magasin, LigneCommandeVente ligne) {
        sortieStockService.consumeForVente(new SortieStockForVente(
                magasin, ligne.getProductFournisseur(),
                ligne.getQuantite(), ligne.getPrixUnitaire(),
                ligne
        ));
    }

    /** Applique un éventuel premier paiement à la facture (création + mise à jour montantPaye/statut). */
    public FactureClient applyPremierPaiementIfPresent(PaiementVenteRequest premierPaiement, FactureClient facture) {
        if (premierPaiement == null) {
            return facture;
        }

        LocalDate datePaiement = premierPaiement.datePaiement() != null ? premierPaiement.datePaiement() : LocalDate.now();
        paiementVenteDomainService.create(new PaiementVenteCreate(
                facture, premierPaiement.montant(), premierPaiement.modePaiementAsEnum(), datePaiement
        ));
        return factureClientDomainService.applyPaiement(facture, premierPaiement.montant());
    }

    /** Vérifie que la commande est dans un statut annulable (VALIDATE) — throw BadArgument sinon. */
    public void ensureCancellable(CommandeVente commande) {
        CommandeVenteStatut statut = commande.getStatut();
        if (statut == CommandeVenteStatut.CANCEL) {
            throw new BadArgumentException("commandeVente.cancel.alreadyCancelled");
        }
        if (statut != CommandeVenteStatut.VALIDATE) {
            throw new BadArgumentException("commandeVente.cancel.notValidated", statut.name());
        }
    }

    /** Vérifie que la commande est encore dans la fenêtre d'annulation autorisée (configurable via SaleProperties). */
    public void ensureWithinCancelWindow(CommandeVente commande) {
        int maxHours = saleProperties.cancelWindowHours();
        LocalDateTime deadline = commande.getCreatedAt().plusHours(maxHours);
        if (deadline.isBefore(LocalDateTime.now())) {
            throw new BadArgumentException("commandeVente.cancel.windowExpired", maxHours);
        }
    }

    /** Récupère les sorties actives d'une ligne et les ré-injecte une par une (1 mouvement RETOUR_CLIENT par lot). */
    public ReinjectionStockResult reinjectStockForLigne(LigneCommandeVente ligne) {
        List<SortieStock> sorties = sortieStockDomainService.findActiveByLigneVenteId(ligne.getId());

        if (sorties.isEmpty()) {
            return ReinjectionStockResult.empty();
        }

        EntreeStock firstLot = sorties.get(0).getEntreeStock();
        Stock stock = stockDomainService.findByMagasinIdAndProduitId(firstLot.getMagasin().getId(), firstLot.getProduit().getId())
                .orElseThrow(() -> new EntityException("stock.notFound"));

        int totalQuantite = sorties.stream()
                .mapToInt(sortie -> reinjectOneSortie(sortie, stock))
                .sum();

        return new ReinjectionStockResult(totalQuantite, sorties.size());
    }

    /** Recrédit le lot d'origine, marque la sortie comme annulée, incrémente le stock agrégé et journalise un RETOUR_CLIENT. */
    public int reinjectOneSortie(SortieStock sortie, Stock stock) {
        EntreeStock lot = sortie.getEntreeStock();
        int quantite = sortie.getQuantiteSortie();
        int stockAvant = stock.getQuantiteDisponible();

        entreeStockDomainService.creditQuantiteRestante(lot, quantite);
        sortieStockDomainService.markAsAnnulee(sortie);
        Stock updated = stockDomainService.creditQuantite(stock, quantite);

        mouvementStockDomainService.journalize(updated, new MouvementJournalize(
                MouvementStockType.RETOUR_CLIENT,
                quantite,
                stockAvant,
                updated.getQuantiteDisponible(),
                null,
                null
        ));
        return quantite;
    }

}
