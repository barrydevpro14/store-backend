package org.store.achat.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.dto.AchatContext;
import org.store.achat.application.dto.AchatDetailsResponse;
import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatResponse;
import org.store.achat.application.dto.CommandeAchatCreate;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.application.dto.FactureAchatCreate;
import org.store.achat.application.dto.FactureAchatResponse;
import org.store.achat.application.dto.LigneAchatRequest;
import org.store.achat.application.dto.LigneCommandeAchatCreate;
import org.store.achat.application.dto.LigneCommandeAchatResponse;
import org.store.achat.application.service.IAchatService;
import org.store.achat.application.service.ICommandeAchatService;
import org.store.achat.application.service.IFactureAchatService;
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
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.stock.application.dto.EntreeStockCreate;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.EntreeStockDomainService;
import org.store.stock.domain.service.MouvementStockDomainService;
import org.store.stock.domain.service.StockDomainService;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Orchestre la création atomique d'un achat : commande + lignes + facture + entrées stock + journal.
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
    private final IFactureAchatService factureAchatService;
    private final ValidatorService validatorService;

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
                            IFactureAchatService factureAchatService,
                            ValidatorService validatorService) {
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
        this.factureAchatService = factureAchatService;
        this.validatorService = validatorService;
    }

    /** Vérifie scoping/cohérence, crée commande + lignes + facture, alimente le stock et journalise. */
    @Override
    @Transactional
    public AchatResponse create(AchatRequest achatRequest) {
        validatorService.validate(achatRequest);

        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(achatRequest.magasinId()));
        Fournisseur fournisseur = fournisseurService.ensureBelongsToCurrentEntreprise(fournisseurService.findById(achatRequest.fournisseurId()));

        List<ProductFournisseur> productFournisseurs = resolveAndValidateProductFournisseurs(achatRequest, fournisseur);

        CommandeAchat commande = commandeAchatDomainService.create(new CommandeAchatCreate(
                fournisseur, magasin, achatRequest.dateCommande(),
                commandeAchatDomainService.generateReference(),
                CommandeAchatStatut.RECEPTIONNEE
        ));

        BigDecimal montantTotal = createLignesAndComputeTotal(achatRequest, commande, productFournisseurs);

        FactureAchat facture = factureAchatDomainService.create(new FactureAchatCreate(
                commande, achatRequest.facture().numero(),
                achatRequest.facture().date(), achatRequest.facture().dateEcheance(),
                montantTotal
        ));

        createEntriesAndUpdateStock(new AchatContext(achatRequest, magasin, commande, facture, productFournisseurs));

        return new AchatResponse(
                new CommandeAchatResponse(commande),
                new FactureAchatResponse(facture)
        );
    }

    /** Retourne le détail d'un achat : commande + facture + lignes (produit, quantité, prixAchat, prixVente). Scoping entreprise via la commande. */
    @Override
    public AchatDetailsResponse findDetailsById(UUID commandeId) {
        CommandeAchat commande = commandeAchatService.ensureBelongsToCurrentEntreprise(commandeAchatService.findById(commandeId));
        FactureAchat facture = factureAchatService.findByCommandeId(commande.getId());

        List<LigneCommandeAchatResponse> lignes = commande.getLignes().stream()
                .map(LigneCommandeAchatResponse::new)
                .toList();

        return new AchatDetailsResponse(
                new CommandeAchatResponse(commande),
                new FactureAchatResponse(facture),
                lignes
        );
    }

    /** Résout chaque productFournisseur, vérifie son appartenance entreprise/fournisseur et valide prixVente > prixAchat pour chaque ligne. */
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

    /** Crée chaque ligne de commande (snapshot prixAchat + prixVente), met à jour le prixVente courant du PF, et retourne le montant total cumulé. */
    public BigDecimal createLignesAndComputeTotal(AchatRequest request, CommandeAchat commande, List<ProductFournisseur> productFournisseurs) {
        List<LigneAchatRequest> lignes = request.lignes();
        Iterator<ProductFournisseur> productFournisseurIterator = productFournisseurs.iterator();

        lignes.forEach(ligne -> persistLigneAndApplyPrixVente(commande, ligne, productFournisseurIterator.next()));

        return lignes.stream()
                .map(ligne -> ligne.prixAchat().multiply(BigDecimal.valueOf(ligne.quantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Persiste une LigneCommandeAchat (snapshot prixAchat + prixVente) et met à jour le prixVente courant du PF. */
    public void persistLigneAndApplyPrixVente(CommandeAchat commande, LigneAchatRequest ligne, ProductFournisseur productFournisseur) {
        ligneCommandeAchatDomainService.create(new LigneCommandeAchatCreate(
                commande, productFournisseur, ligne.quantite(), ligne.prixAchat(), ligne.prixVente()
        ));
        productFournisseurService.applyPrixVenteFromPurchase(productFournisseur, ligne.prixVente());
    }

    /** Itère sur les lignes de l'achat et délègue le traitement stock + journal à `processPurchaseLineEntry`. */
    public void createEntriesAndUpdateStock(AchatContext context) {
        Iterator<ProductFournisseur> productFournisseurIterator = context.productFournisseurs().iterator();

        context.request().lignes().forEach(ligne ->
                processPurchaseLineEntry(context, ligne, productFournisseurIterator.next()));
    }

    /** Enregistre le lot, upsert le Stock agrégé du couple (magasin, produit) et journalise le mouvement ENTREE_ACHAT pour une ligne d'achat. */
    public void processPurchaseLineEntry(AchatContext context, LigneAchatRequest ligne, ProductFournisseur productFournisseur) {
        Magasin magasin = context.magasin();
        Product produit = productFournisseur.getProduct();

        int stockAvant = stockDomainService.findByMagasinIdAndProduitId(magasin.getId(), produit.getId())
                .map(Stock::getQuantiteDisponible).orElse(0);

        entreeStockDomainService.create(new EntreeStockCreate(
                magasin, produit, productFournisseur,
                ligne.quantite(), ligne.prixAchat(),
                ligne.numeroLot(), ligne.dateExpiration(),
                context.commande()
        ));

        Stock stock = stockDomainService.createOrUpdateEntry(magasin, produit, ligne.quantite(), ligne.prixAchat());

        mouvementStockDomainService.journalize(stock, new MouvementJournalize(
                MouvementStockType.ENTREE_ACHAT,
                ligne.quantite(), stockAvant, stock.getQuantiteDisponible(),
                context.facture().getNumero(),
                null
        ));
    }
}
