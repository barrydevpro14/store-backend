package org.store.vente.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.dto.UserSummaryResponse;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.common.tools.NameHelper;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.IAccountService;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.SortieStockForVente;
import org.store.stock.application.service.ISortieStockService;
import org.store.users.application.service.IEmployeService;
import org.store.users.domain.model.Employe;
import org.store.vente.application.dto.CommandeVenteCreate;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.application.dto.FactureClientCreate;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.application.dto.LigneCommandeVenteCreate;
import org.store.vente.application.dto.LigneCommandeVenteResponse;
import org.store.vente.application.dto.LigneVenteRequest;
import org.store.vente.application.dto.PaiementVenteCreate;
import org.store.vente.application.dto.PaiementVenteRequest;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.application.dto.VenteContext;
import org.store.vente.application.dto.VenteDetailsResponse;
import org.store.vente.application.dto.VenteRequest;
import org.store.vente.application.dto.VenteResponse;
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
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Orchestre la création atomique d'une vente : commande + lignes + sorties stock FIFO + facture + paiement initial.
 * Le vendeur (= user courant Employe) est tracé via `AuditableEntity.createdBy` (Forbidden si pas un Employe).
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
                            ValidatorService validatorService) {
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
    }

    /** Valide, crée commande + lignes + sorties FIFO + facture, applique le paiement initial éventuel. */
    @Override
    @Transactional
    public VenteResponse create(VenteRequest venteRequest) {
        validatorService.validate(venteRequest);

        Employe user = employeService.findCurrentUser();
        Magasin magasin = user.getMagasin();
        Client client = resolveClient(venteRequest.clientId());
        LocalDate dateVente = LocalDate.now();

        List<ProductFournisseur> productFournisseurs = resolveAndValidateLignes(venteRequest);

        CommandeVente commande = commandeVenteDomainService.create(new CommandeVenteCreate(
                client, magasin, dateVente,
                commandeVenteDomainService.generateReference(),
                CommandeVenteStatut.DELIVERED
        ));

        VenteContext context = new VenteContext(venteRequest, commande, magasin, user, productFournisseurs);
        BigDecimal montantTotal = createLignesAndProcessStock(context);
        commandeVenteDomainService.applyMontantTotal(commande, montantTotal);

        FactureClient facture = factureClientDomainService.create(new FactureClientCreate(
                commande, factureClientDomainService.generateNumero(),
                dateVente, venteRequest.dateEcheance(), montantTotal
        ));

        FactureClient finalFacture = applyPremierPaiementIfPresent(venteRequest.premierPaiement(), facture, commande);

        UserSummaryResponse userSummary = new UserSummaryResponse(user.getId(), NameHelper.formatNomComplet(user.getNom(), user.getPrenom()));
        return new VenteResponse(
                new CommandeVenteResponse(commande, userSummary),
                new FactureClientResponse(finalFacture)
        );
    }

    /** Retourne le détail d'une vente : commande + facture + lignes + paiements (user résolu via createdBy). */
    @Override
    public VenteDetailsResponse findDetailsById(UUID commandeId) {
        CommandeVente commande = ensureBelongsToCurrentEntreprise(commandeVenteDomainService.findById(commandeId));

        FactureClient facture = factureClientDomainService.findByCommandeId(commande.getId())
                .orElseThrow(() -> new EntityException("factureClient.notFoundForCommande", commande.getId()));

        List<PaiementVente> paiements = paiementVenteDomainService.findAllByFactureId(facture.getId());
        UserSummaryResponse user = accountService.findUserSummaryByAccountId(commande.getCreatedBy()).orElse(null);

        List<LigneCommandeVenteResponse> lignes = commande.getLignes().stream()
                .map(LigneCommandeVenteResponse::new)
                .toList();

        List<PaiementVenteResponse> paiementsResponse = paiements.stream()
                .map(PaiementVenteResponse::new)
                .toList();

        return new VenteDetailsResponse(
                new CommandeVenteResponse(commande, user),
                new FactureClientResponse(facture),
                lignes,
                paiementsResponse
        );
    }

    /** Lève `ForbiddenException` si la commande n'appartient pas à l'entreprise du caller (via magasin.entreprise). */
    public CommandeVente ensureBelongsToCurrentEntreprise(CommandeVente commande) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (!commande.getMagasin().getEntreprise().getId().equals(currentUser.entrepriseId())) {
            throw new ForbiddenException("commandeVente.notOwned");
        }
        return commande;
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

        if (ligne.prixUnitaire().compareTo(productFournisseur.getPrixVente()) < 0) {
            throw new BadArgumentException("vente.prixUnitaire.belowFloor", productFournisseur.getPrixVente().toString());
        }
        return productFournisseur;
    }

    /** Persiste chaque ligne, déclenche la sortie stock FIFO du PF concerné et retourne le montant total cumulé. */
    public BigDecimal createLignesAndProcessStock(VenteContext context) {
        List<LigneVenteRequest> lignes = context.request().lignes();
        Iterator<ProductFournisseur> productFournisseurIterator = context.productFournisseurs().iterator();

        lignes.forEach(ligne -> processVenteLine(context, ligne, productFournisseurIterator.next()));

        return lignes.stream()
                .map(ligne -> ligne.prixUnitaire().multiply(BigDecimal.valueOf(ligne.quantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Crée la ligne de vente puis consomme les lots FIFO du PF en liant chaque SortieStock à cette ligne. */
    public void processVenteLine(VenteContext context, LigneVenteRequest ligneRequest, ProductFournisseur productFournisseur) {
        LigneCommandeVente ligne = ligneCommandeVenteDomainService.create(new LigneCommandeVenteCreate(
                context.commande(), productFournisseur, ligneRequest.quantite(), ligneRequest.prixUnitaire()
        ));

        sortieStockService.consumeForVente(new SortieStockForVente(
                context.magasin(), productFournisseur,
                ligneRequest.quantite(), ligneRequest.prixUnitaire(),
                ligne
        ));
    }

    /** Applique un éventuel premier paiement à la facture (création + mise à jour montantPaye/statut) et propage sur la commande. */
    public FactureClient applyPremierPaiementIfPresent(PaiementVenteRequest premierPaiement, FactureClient facture, CommandeVente commande) {
        if (premierPaiement == null) {
            return facture;
        }

        LocalDate datePaiement = premierPaiement.datePaiement() != null ? premierPaiement.datePaiement() : LocalDate.now();
        paiementVenteDomainService.create(new PaiementVenteCreate(
                facture, premierPaiement.montant(), premierPaiement.modePaiementAsEnum(), datePaiement
        ));
        FactureClient updated = factureClientDomainService.applyPaiement(facture, premierPaiement.montant());
        commandeVenteDomainService.applyMontantPaye(commande, premierPaiement.montant());
        return updated;
    }

}
