package org.store.vente.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.achat.domain.enums.StatutFacture;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.dto.UserSummaryResponse;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.produit.domain.model.Quality;
import org.store.property.SaleProperties;
import org.store.security.application.dto.UserPrincipal;
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
import org.store.vente.application.dto.LigneVenteRequest;
import org.store.vente.application.dto.PaiementVenteCreate;
import org.store.vente.application.dto.PaiementVenteRequest;
import org.store.vente.application.dto.VenteDetailsResponse;
import org.store.vente.application.dto.VenteRequest;
import org.store.vente.application.dto.VenteResponse;
import org.store.vente.application.service.impl.VenteServiceImpl;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.enums.MotifAnnulationVente;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.model.LigneCommandeVente;
import org.store.vente.domain.service.CommandeVenteDomainService;
import org.store.vente.domain.service.FactureClientDomainService;
import org.store.vente.domain.service.LigneCommandeVenteDomainService;
import org.store.vente.domain.service.PaiementVenteDomainService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenteServiceImplTest {

    @Mock private CommandeVenteDomainService commandeVenteDomainService;
    @Mock private LigneCommandeVenteDomainService ligneCommandeVenteDomainService;
    @Mock private FactureClientDomainService factureClientDomainService;
    @Mock private PaiementVenteDomainService paiementVenteDomainService;
    @Mock private IEmployeService employeService;
    @Mock private IClientService clientService;
    @Mock private IProductFournisseurService productFournisseurService;
    @Mock private ISortieStockService sortieStockService;
    @Mock private IAccountService accountService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;
    @Mock private EntreeStockDomainService entreeStockDomainService;
    @Mock private SortieStockDomainService sortieStockDomainService;
    @Mock private StockDomainService stockDomainService;
    @Mock private MouvementStockDomainService mouvementStockDomainService;
    @Mock private SaleProperties saleProperties;

    @InjectMocks
    private VenteServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private UUID employeId;
    private UUID productFournisseurId;
    private Entreprise entreprise;
    private Magasin magasin;
    private Employe vendeur;
    private Product produit;
    private ProductFournisseur productFournisseur;
    private CommandeVente commande;
    private FactureClient facture;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
        employeId = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);

        vendeur = new Employe();
        vendeur.setId(employeId);
        vendeur.setNom("Diop");
        vendeur.setPrenom("Awa");
        vendeur.setMagasin(magasin);

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(UUID.randomUUID());
        fournisseur.setNom("Chine SARL");

        Quality quality = new Quality();
        quality.setId(UUID.randomUUID());
        quality.setLibelle("Original");

        produit = new Product();
        produit.setId(UUID.randomUUID());
        produit.setNom("Clou 10mm");
        produit.setReference("CL-10");
        produit.setEntreprise(entreprise);

        productFournisseur = new ProductFournisseur();
        productFournisseur.setId(productFournisseurId);
        productFournisseur.setProduct(produit);
        productFournisseur.setFournisseur(fournisseur);
        productFournisseur.setQuality(quality);
        productFournisseur.setPrixAchat(new BigDecimal("8.00"));
        productFournisseur.setPrixVente(new BigDecimal("10.00"));

        commande = new CommandeVente();
        commande.setId(UUID.randomUUID());
        commande.setReference("VTE-AUTO");
        commande.setStatut(CommandeVenteStatut.DELIVERED);
        commande.setMagasin(magasin);
        commande.setLignes(List.of());

        facture = new FactureClient();
        facture.setId(UUID.randomUUID());
        facture.setCommande(commande);
        facture.setNumero("FAC-VTE-AUTO");
        facture.setMontantTotal(new BigDecimal("1000.00"));
        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setStatut(StatutFacture.NON_PAYEE);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, null, "owner", "PROPRIETAIRE", List.of("SALE_READ"));
    }

    private VenteRequest sampleRequest() {
        return new VenteRequest(
                null,
                LocalDate.of(2026, 5, 16),
                List.of(new LigneVenteRequest(productFournisseurId, 100, new BigDecimal("10.00"))),
                null
        );
    }

    @Test
    void create_should_orchestrate_sale_flow() {
        VenteRequest req = sampleRequest();

        when(employeService.findCurrentUser()).thenReturn(vendeur);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(commandeVenteDomainService.generateReference()).thenReturn("VTE-AUTO");
        when(commandeVenteDomainService.create(any())).thenReturn(commande);
        when(ligneCommandeVenteDomainService.create(any())).thenReturn(new LigneCommandeVente());
        when(factureClientDomainService.generateNumero()).thenReturn("FAC-VTE-AUTO");
        when(factureClientDomainService.create(any())).thenReturn(facture);

        VenteResponse response = service.create(req);

        assertThat(response.commande().reference()).isEqualTo("VTE-AUTO");
        assertThat(response.commande().user()).isNotNull();
        assertThat(response.commande().user().nomComplet()).isEqualTo("Diop Awa");
        assertThat(response.facture().numero()).isEqualTo("FAC-VTE-AUTO");
        verify(sortieStockService).consumeForVente(any(SortieStockForVente.class));
    }

    @Test
    void create_should_set_dateVente_to_today() {
        VenteRequest req = sampleRequest();

        when(employeService.findCurrentUser()).thenReturn(vendeur);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(commandeVenteDomainService.generateReference()).thenReturn("VTE-AUTO");
        when(commandeVenteDomainService.create(any())).thenReturn(commande);
        when(ligneCommandeVenteDomainService.create(any())).thenReturn(new LigneCommandeVente());
        when(factureClientDomainService.generateNumero()).thenReturn("FAC-VTE-AUTO");
        when(factureClientDomainService.create(any())).thenReturn(facture);

        ArgumentCaptor<CommandeVenteCreate> commandeCaptor = ArgumentCaptor.forClass(CommandeVenteCreate.class);
        service.create(req);

        verify(commandeVenteDomainService).create(commandeCaptor.capture());
        assertThat(commandeCaptor.getValue().dateVente()).isEqualTo(LocalDate.now());
    }

    @Test
    void create_should_throw_when_user_not_employe() {
        when(employeService.findCurrentUser()).thenThrow(new ForbiddenException("vente.user.required"));

        assertThatThrownBy(() -> service.create(sampleRequest()))
                .isInstanceOf(ForbiddenException.class);

        verify(commandeVenteDomainService, never()).create(any());
    }

    @Test
    void create_should_throw_when_prix_below_floor() {
        VenteRequest req = new VenteRequest(
                null,
                LocalDate.of(2026, 5, 16),
                List.of(new LigneVenteRequest(productFournisseurId, 100, new BigDecimal("5.00"))),
                null
        );

        when(employeService.findCurrentUser()).thenReturn(vendeur);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadArgumentException.class);

        verify(commandeVenteDomainService, never()).create(any());
    }

    @Test
    void create_should_apply_premier_paiement_when_present() {
        VenteRequest req = new VenteRequest(
                null,
                LocalDate.of(2026, 5, 16),
                List.of(new LigneVenteRequest(productFournisseurId, 100, new BigDecimal("10.00"))),
                new PaiementVenteRequest(new BigDecimal("500.00"), MoyenPaiement.CASH.name(), null)
        );

        FactureClient factureAfterPaiement = new FactureClient();
        factureAfterPaiement.setId(facture.getId());
        factureAfterPaiement.setCommande(commande);
        factureAfterPaiement.setNumero("FAC-VTE-AUTO");
        factureAfterPaiement.setMontantTotal(new BigDecimal("1000.00"));
        factureAfterPaiement.setMontantPaye(new BigDecimal("500.00"));
        factureAfterPaiement.setStatut(StatutFacture.PARTIELLEMENT_PAYEE);

        when(employeService.findCurrentUser()).thenReturn(vendeur);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(commandeVenteDomainService.generateReference()).thenReturn("VTE-AUTO");
        when(commandeVenteDomainService.create(any())).thenReturn(commande);
        when(ligneCommandeVenteDomainService.create(any())).thenReturn(new LigneCommandeVente());
        when(factureClientDomainService.generateNumero()).thenReturn("FAC-VTE-AUTO");
        when(factureClientDomainService.create(any())).thenReturn(facture);
        when(factureClientDomainService.applyPaiement(facture, new BigDecimal("500.00"))).thenReturn(factureAfterPaiement);

        VenteResponse response = service.create(req);

        ArgumentCaptor<PaiementVenteCreate> paiementCaptor = ArgumentCaptor.forClass(PaiementVenteCreate.class);
        assertThat(response.facture().statut()).isEqualTo(StatutFacture.PARTIELLEMENT_PAYEE);
        verify(paiementVenteDomainService).create(paiementCaptor.capture());
        PaiementVenteCreate captured = paiementCaptor.getValue();
        assertThat(captured.facture()).isSameAs(facture);
        assertThat(captured.montant()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(captured.moyen()).isEqualTo(MoyenPaiement.CASH);
        assertThat(captured.datePaiement()).isEqualTo(LocalDate.now());
    }

    @Test
    void create_should_use_datePaiement_from_request_when_provided() {
        LocalDate datePaiementSaisi = LocalDate.of(2026, 5, 10);
        VenteRequest req = new VenteRequest(
                null,
                LocalDate.of(2026, 5, 16),
                List.of(new LigneVenteRequest(productFournisseurId, 100, new BigDecimal("10.00"))),
                new PaiementVenteRequest(new BigDecimal("500.00"), MoyenPaiement.CASH.name(), datePaiementSaisi)
        );

        when(employeService.findCurrentUser()).thenReturn(vendeur);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(commandeVenteDomainService.generateReference()).thenReturn("VTE-AUTO");
        when(commandeVenteDomainService.create(any())).thenReturn(commande);
        when(ligneCommandeVenteDomainService.create(any())).thenReturn(new LigneCommandeVente());
        when(factureClientDomainService.generateNumero()).thenReturn("FAC-VTE-AUTO");
        when(factureClientDomainService.create(any())).thenReturn(facture);
        when(factureClientDomainService.applyPaiement(facture, new BigDecimal("500.00"))).thenReturn(facture);

        service.create(req);

        ArgumentCaptor<PaiementVenteCreate> paiementCaptor = ArgumentCaptor.forClass(PaiementVenteCreate.class);
        verify(paiementVenteDomainService).create(paiementCaptor.capture());
        assertThat(paiementCaptor.getValue().datePaiement()).isEqualTo(datePaiementSaisi);
    }

    @Test
    void findDetailsById_should_return_commande_facture_lignes_paiements() {
        UUID commandeId = commande.getId();
        commande.setCreatedBy(UUID.randomUUID().toString());

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(factureClientDomainService.findByCommandeId(commandeId)).thenReturn(Optional.of(facture));
        when(paiementVenteDomainService.findAllByFactureId(facture.getId())).thenReturn(List.of());
        when(accountService.findUserSummaryByAccountId(commande.getCreatedBy()))
                .thenReturn(Optional.of(new UserSummaryResponse(employeId, "Diop Awa")));

        VenteDetailsResponse response = service.findDetailsById(commandeId);

        assertThat(response.commande().reference()).isEqualTo("VTE-AUTO");
        assertThat(response.commande().user().nomComplet()).isEqualTo("Diop Awa");
        assertThat(response.facture().numero()).isEqualTo("FAC-VTE-AUTO");
        assertThat(response.lignes()).isEmpty();
        assertThat(response.paiements()).isEmpty();
    }

    @Test
    void findDetailsById_should_throw_when_facture_missing() {
        UUID commandeId = commande.getId();
        commande.setCreatedBy(UUID.randomUUID().toString());

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(factureClientDomainService.findByCommandeId(commandeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findDetailsById(commandeId))
                .isInstanceOf(org.store.common.exceptions.EntityException.class);

        verify(paiementVenteDomainService, never()).findAllByFactureId(any());
    }

    @Test
    void findDetailsById_should_throw_when_not_owned() {
        UUID otherEntrepriseId = UUID.randomUUID();
        Entreprise other = new Entreprise();
        other.setId(otherEntrepriseId);
        Magasin foreignMagasin = new Magasin();
        foreignMagasin.setId(UUID.randomUUID());
        foreignMagasin.setEntreprise(other);
        commande.setMagasin(foreignMagasin);

        when(commandeVenteDomainService.findById(commande.getId())).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThatThrownBy(() -> service.findDetailsById(commande.getId()))
                .isInstanceOf(ForbiddenException.class);

        verify(factureClientDomainService, never()).findByCommandeId(any());
    }

    private CommandeVente deliveredCommandeWithOneLigne(int quantiteSortie) {
        UUID ligneId = UUID.randomUUID();
        LigneCommandeVente ligne = new LigneCommandeVente();
        ligne.setId(ligneId);
        commande.setStatut(CommandeVenteStatut.DELIVERED);
        commande.setCreatedAt(LocalDateTime.now().minusHours(1));
        commande.setLignes(List.of(ligne));
        return commande;
    }

    @Test
    void cancel_should_reinject_stock_and_mark_cancelled() {
        CommandeVente delivered = deliveredCommandeWithOneLigne(8);
        UUID ligneId = delivered.getLignes().get(0).getId();

        EntreeStock lot = new EntreeStock();
        lot.setId(UUID.randomUUID());
        lot.setMagasin(magasin);
        lot.setProduit(produit);
        lot.setQuantiteRestante(0);

        SortieStock sortie = new SortieStock();
        sortie.setEntreeStock(lot);
        sortie.setQuantiteSortie(8);
        sortie.setAnnulee(false);

        Stock stock = new Stock();
        stock.setQuantiteDisponible(2);

        when(commandeVenteDomainService.findById(delivered.getId())).thenReturn(delivered);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(saleProperties.cancelWindowHours()).thenReturn(24);
        when(sortieStockDomainService.findActiveByLigneVenteId(ligneId)).thenReturn(List.of(sortie));
        when(stockDomainService.findByMagasinIdAndProduitId(magasinId, produit.getId())).thenReturn(Optional.of(stock));
        when(stockDomainService.creditQuantite(eq(stock), eq(8))).thenAnswer(inv -> {
            stock.setQuantiteDisponible(stock.getQuantiteDisponible() + 8);
            return stock;
        });
        when(commandeVenteDomainService.cancel(any(), any(), any())).thenAnswer(inv -> {
            CommandeVente arg = inv.getArgument(0);
            arg.setStatut(CommandeVenteStatut.ANNULEE);
            arg.setMotifAnnulation(inv.getArgument(1));
            arg.setCommentaireAnnulation(inv.getArgument(2));
            arg.setDateAnnulation(LocalDateTime.now());
            return arg;
        });
        when(factureClientDomainService.findByCommandeId(delivered.getId())).thenReturn(Optional.of(facture));

        AnnulationVenteRequest req = new AnnulationVenteRequest("ERREUR_SAISIE", "Saisie incorrecte");
        AnnulationVenteResponse response = service.cancel(delivered.getId(), req);

        assertThat(response.totalQuantiteReinjectee()).isEqualTo(8);
        assertThat(response.nombreMouvementsCrees()).isEqualTo(1);
        assertThat(response.statut()).isEqualTo(CommandeVenteStatut.ANNULEE);
        assertThat(response.motif()).isEqualTo(MotifAnnulationVente.ERREUR_SAISIE);

        verify(entreeStockDomainService).creditQuantiteRestante(lot, 8);
        verify(sortieStockDomainService).markAsAnnulee(sortie);
        verify(stockDomainService).creditQuantite(stock, 8);

        ArgumentCaptor<MouvementJournalize> mouvementCaptor = ArgumentCaptor.forClass(MouvementJournalize.class);
        verify(mouvementStockDomainService).journalize(eq(stock), mouvementCaptor.capture());
        assertThat(mouvementCaptor.getValue().type()).isEqualTo(MouvementStockType.RETOUR_CLIENT);
        assertThat(mouvementCaptor.getValue().quantite()).isEqualTo(8);
        assertThat(mouvementCaptor.getValue().stockAvant()).isEqualTo(2);
        assertThat(mouvementCaptor.getValue().stockApres()).isEqualTo(10);

        verify(factureClientDomainService).cancel(facture);
    }

    @Test
    void cancel_should_throw_when_already_cancelled() {
        commande.setStatut(CommandeVenteStatut.ANNULEE);
        commande.setCreatedAt(LocalDateTime.now().minusHours(1));

        when(commandeVenteDomainService.findById(commande.getId())).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        AnnulationVenteRequest req = new AnnulationVenteRequest("ERREUR_SAISIE", null);

        assertThatThrownBy(() -> service.cancel(commande.getId(), req))
                .isInstanceOf(BadArgumentException.class);

        verify(commandeVenteDomainService, never()).cancel(any(), any(), any());
        verify(stockDomainService, never()).creditQuantite(any(), anyInt());
    }

    @Test
    void cancel_should_throw_when_not_delivered() {
        commande.setStatut(CommandeVenteStatut.NOT_DELIVERED);
        commande.setCreatedAt(LocalDateTime.now().minusHours(1));

        when(commandeVenteDomainService.findById(commande.getId())).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        AnnulationVenteRequest req = new AnnulationVenteRequest("ERREUR_SAISIE", null);

        assertThatThrownBy(() -> service.cancel(commande.getId(), req))
                .isInstanceOf(BadArgumentException.class);

        verify(commandeVenteDomainService, never()).cancel(any(), any(), any());
    }

    @Test
    void cancel_should_throw_when_window_expired() {
        commande.setStatut(CommandeVenteStatut.DELIVERED);
        commande.setCreatedAt(LocalDateTime.now().minusHours(48));

        when(commandeVenteDomainService.findById(commande.getId())).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(saleProperties.cancelWindowHours()).thenReturn(24);

        AnnulationVenteRequest req = new AnnulationVenteRequest("ERREUR_SAISIE", null);

        assertThatThrownBy(() -> service.cancel(commande.getId(), req))
                .isInstanceOf(BadArgumentException.class);

        verify(commandeVenteDomainService, never()).cancel(any(), any(), any());
    }

    @Test
    void cancel_should_throw_when_cross_entreprise() {
        commande.setStatut(CommandeVenteStatut.DELIVERED);
        commande.setCreatedAt(LocalDateTime.now().minusHours(1));

        UUID otherEntrepriseId = UUID.randomUUID();
        Entreprise other = new Entreprise();
        other.setId(otherEntrepriseId);
        Magasin foreignMagasin = new Magasin();
        foreignMagasin.setId(UUID.randomUUID());
        foreignMagasin.setEntreprise(other);
        commande.setMagasin(foreignMagasin);

        when(commandeVenteDomainService.findById(commande.getId())).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        AnnulationVenteRequest req = new AnnulationVenteRequest("ERREUR_SAISIE", null);

        assertThatThrownBy(() -> service.cancel(commande.getId(), req))
                .isInstanceOf(ForbiddenException.class);

        verify(commandeVenteDomainService, never()).cancel(any(), any(), any());
    }
}
