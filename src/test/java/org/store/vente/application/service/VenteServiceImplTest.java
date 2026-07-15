package org.store.vente.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.paiement.application.service.IMoyenPaiementService;
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
import org.store.vente.application.dto.LigneVenteUpdateRequest;
import org.store.vente.application.dto.PaiementVenteCreate;
import org.store.vente.application.dto.PaiementVenteRequest;
import org.store.vente.application.dto.VenteDetailsResponse;
import org.store.vente.application.dto.VenteDraftResponse;
import org.store.vente.application.dto.VenteRequest;
import org.store.vente.application.dto.VenteResponse;
import org.store.vente.application.dto.VenteValidateRequest;
import org.store.vente.application.service.impl.VenteServiceImpl;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.enums.MotifAnnulationVente;
import org.store.vente.domain.model.Client;
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
import java.util.ArrayList;
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
    @Mock private org.store.notification.application.service.INotificationEventPublisher notificationEventPublisher;
    @Mock private org.store.audit.application.service.IAuditEventPublisher auditEventPublisher;
    @Mock private IMoyenPaiementService moyenPaiementService;
    @Mock private org.store.sequence.application.service.IDocumentSequenceService documentSequenceService;

    private static final UUID MOYEN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private org.store.paiement.domain.model.MoyenPaiement moyenCash() {
        org.store.paiement.domain.model.MoyenPaiement m = new org.store.paiement.domain.model.MoyenPaiement();
        m.setId(MOYEN_ID); m.setLibelle("Espèces"); m.setCode("CASH");
        return m;
    }

    @InjectMocks
    private VenteServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private UUID employeId;
    private UUID vendeurAccountId;
    private UUID productFournisseurId;
    private UUID commandeId;
    private UUID ligneId;
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
        vendeurAccountId = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();
        commandeId = UUID.randomUUID();
        ligneId = UUID.randomUUID();

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

        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setNom("Test Client");
        client.setMagasin(magasin);

        commande = new CommandeVente();
        commande.setId(commandeId);
        commande.setReference("VTE-AUTO");
        commande.setStatut(CommandeVenteStatut.DRAFT);
        commande.setMagasin(magasin);
        commande.setClient(client);
        commande.setCreatedBy(vendeurAccountId.toString());
        commande.setLignes(new ArrayList<>());

        facture = new FactureClient();
        facture.setId(UUID.randomUUID());
        facture.setCommande(commande);
        facture.setNumero("FAC-VTE-AUTO");
        facture.setMontantTotal(new BigDecimal("1000.00"));
        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setStatut(StatutFacture.NON_PAYEE);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(vendeurAccountId, UUID.randomUUID(), entrepriseId, null, "owner", null, null, "OWNER", List.of("SALE_READ"));
    }

    private VenteRequest sampleRequest() {
        return new VenteRequest(
                null,
                List.of(new LigneVenteRequest(productFournisseur.getProduct().getId(), productFournisseur.getQuality().getId(), productFournisseur.getFournisseur().getId(), 100, new BigDecimal("10.00")))
        );
    }

    private VenteValidateRequest sampleValidateRequest() {
        return new VenteValidateRequest(LocalDate.now().plusDays(30), null);
    }

    private LigneCommandeVente sampleLigne(int quantite, BigDecimal prixUnitaire) {
        LigneCommandeVente ligne = new LigneCommandeVente();
        ligne.setId(ligneId);
        ligne.setCommande(commande);
        ligne.setProductFournisseur(productFournisseur);
        ligne.setQuantite(quantite);
        ligne.setPrixUnitaire(prixUnitaire);
        ligne.setMontantTotal(prixUnitaire.multiply(BigDecimal.valueOf(quantite)));
        return ligne;
    }

    @Test
    void create_should_persist_draft_without_stock_or_facture() {
        VenteRequest req = sampleRequest();

        when(employeService.findCurrentUser()).thenReturn(vendeur);
        when(productFournisseurService.findByTriplet(any(), any(), any())).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(documentSequenceService.generateReference(any(), eq(org.store.sequence.domain.enums.TypeDocument.COMMANDE_CLIENT))).thenReturn("VTE-AUTO");
        when(commandeVenteDomainService.create(any())).thenReturn(commande);
        when(commandeVenteDomainService.findById(commande.getId())).thenReturn(commande);
        when(ligneCommandeVenteDomainService.create(any())).thenReturn(sampleLigne(100, new BigDecimal("10.00")));

        VenteDraftResponse response = service.create(req);

        assertThat(response.commande().reference()).isEqualTo("VTE-AUTO");
        assertThat(response.commande().statut()).isEqualTo(CommandeVenteStatut.DRAFT);

        ArgumentCaptor<CommandeVenteCreate> captor = ArgumentCaptor.forClass(CommandeVenteCreate.class);
        verify(commandeVenteDomainService).create(captor.capture());
        assertThat(captor.getValue().statut()).isEqualTo(CommandeVenteStatut.DRAFT);

        verify(factureClientDomainService, never()).create(any());
        verify(sortieStockService, never()).consumeForVente(any());
        verify(paiementVenteDomainService, never()).create(any());
    }

    @Test
    void create_should_set_dateVente_to_today() {
        when(employeService.findCurrentUser()).thenReturn(vendeur);
        when(productFournisseurService.findByTriplet(any(), any(), any())).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(documentSequenceService.generateReference(any(), eq(org.store.sequence.domain.enums.TypeDocument.COMMANDE_CLIENT))).thenReturn("VTE-AUTO");
        when(commandeVenteDomainService.create(any())).thenReturn(commande);
        when(commandeVenteDomainService.findById(commande.getId())).thenReturn(commande);
        when(ligneCommandeVenteDomainService.create(any())).thenReturn(sampleLigne(100, new BigDecimal("10.00")));

        ArgumentCaptor<CommandeVenteCreate> commandeCaptor = ArgumentCaptor.forClass(CommandeVenteCreate.class);
        service.create(sampleRequest());

        verify(commandeVenteDomainService).create(commandeCaptor.capture());
        assertThat(commandeCaptor.getValue().dateVente()).isEqualTo(LocalDate.now());
    }

    @Test
    void create_should_throw_when_user_not_employe() {
        when(employeService.findCurrentUser()).thenThrow(new ForbiddenException("vente.user.required"));

        VenteRequest venteReq = sampleRequest();

        assertThatThrownBy(() -> service.create(venteReq))
                .isInstanceOf(ForbiddenException.class);

        verify(commandeVenteDomainService, never()).create(any());
    }

    @Test
    void create_should_throw_when_prix_below_floor() {
        VenteRequest req = new VenteRequest(
                null,
                List.of(new LigneVenteRequest(productFournisseur.getProduct().getId(), productFournisseur.getQuality().getId(), productFournisseur.getFournisseur().getId(), 100, new BigDecimal("5.00")))
        );

        when(employeService.findCurrentUser()).thenReturn(vendeur);
        when(productFournisseurService.findByTriplet(any(), any(), any())).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadArgumentException.class);

        verify(commandeVenteDomainService, never()).create(any());
    }

    @Test
    void validate_should_consume_stock_and_create_facture_and_switch_status() {
        LigneCommandeVente ligne = sampleLigne(100, new BigDecimal("10.00"));
        commande.setLignes(List.of(ligne));
        commande.setDate(LocalDate.now());

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(documentSequenceService.generateReference(any(), eq(org.store.sequence.domain.enums.TypeDocument.FACTURE_CLIENT))).thenReturn("FAC-VTE-AUTO");
        when(factureClientDomainService.create(any())).thenReturn(facture);
        when(commandeVenteDomainService.validate(commande)).thenAnswer(inv -> {
            commande.setStatut(CommandeVenteStatut.VALIDATE);
            return commande;
        });

        VenteResponse response = service.validate(commandeId, sampleValidateRequest());

        assertThat(response.commande().statut()).isEqualTo(CommandeVenteStatut.VALIDATE);
        assertThat(response.facture().numero()).isEqualTo("FAC-VTE-AUTO");

        verify(sortieStockService).consumeForVente(any(SortieStockForVente.class));
        verify(commandeVenteDomainService).validate(commande);
    }

    @Test
    void validate_should_apply_premier_paiement_when_present() {
        LigneCommandeVente ligne = sampleLigne(100, new BigDecimal("10.00"));
        commande.setLignes(List.of(ligne));
        commande.setDate(LocalDate.now());

        VenteValidateRequest req = new VenteValidateRequest(
                LocalDate.now().plusDays(30),
                new PaiementVenteRequest(new BigDecimal("500.00"), MOYEN_ID, null)
        );

        FactureClient factureAfterPaiement = new FactureClient();
        factureAfterPaiement.setId(facture.getId());
        factureAfterPaiement.setCommande(commande);
        factureAfterPaiement.setNumero("FAC-VTE-AUTO");
        factureAfterPaiement.setMontantTotal(new BigDecimal("1000.00"));
        factureAfterPaiement.setMontantPaye(new BigDecimal("500.00"));
        factureAfterPaiement.setStatut(StatutFacture.PARTIELLEMENT_PAYEE);

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(moyenPaiementService.findById(MOYEN_ID)).thenReturn(moyenCash());
        when(documentSequenceService.generateReference(any(), eq(org.store.sequence.domain.enums.TypeDocument.FACTURE_CLIENT))).thenReturn("FAC-VTE-AUTO");
        when(factureClientDomainService.create(any())).thenReturn(facture);
        when(factureClientDomainService.applyPaiement(facture, new BigDecimal("500.00"))).thenReturn(factureAfterPaiement);
        when(commandeVenteDomainService.validate(commande)).thenReturn(commande);

        VenteResponse response = service.validate(commandeId, req);

        ArgumentCaptor<PaiementVenteCreate> paiementCaptor = ArgumentCaptor.forClass(PaiementVenteCreate.class);
        assertThat(response.facture().statut()).isEqualTo(StatutFacture.PARTIELLEMENT_PAYEE);
        verify(paiementVenteDomainService).create(paiementCaptor.capture());
        PaiementVenteCreate captured = paiementCaptor.getValue();
        assertThat(captured.montant()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(captured.moyen().getCode()).isEqualTo("CASH");
        assertThat(captured.datePaiement()).isEqualTo(LocalDate.now());
    }

    @Test
    void validate_should_use_datePaiement_from_request_when_provided() {
        LigneCommandeVente ligne = sampleLigne(100, new BigDecimal("10.00"));
        commande.setLignes(List.of(ligne));
        commande.setDate(LocalDate.now());

        LocalDate datePaiementSaisi = LocalDate.of(2026, 5, 10);
        VenteValidateRequest req = new VenteValidateRequest(
                LocalDate.now().plusDays(30),
                new PaiementVenteRequest(new BigDecimal("500.00"), MOYEN_ID, datePaiementSaisi)
        );

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(moyenPaiementService.findById(MOYEN_ID)).thenReturn(moyenCash());
        when(documentSequenceService.generateReference(any(), eq(org.store.sequence.domain.enums.TypeDocument.FACTURE_CLIENT))).thenReturn("FAC-VTE-AUTO");
        when(factureClientDomainService.create(any())).thenReturn(facture);
        when(factureClientDomainService.applyPaiement(facture, new BigDecimal("500.00"))).thenReturn(facture);
        when(commandeVenteDomainService.validate(commande)).thenReturn(commande);

        service.validate(commandeId, req);

        ArgumentCaptor<PaiementVenteCreate> paiementCaptor = ArgumentCaptor.forClass(PaiementVenteCreate.class);
        verify(paiementVenteDomainService).create(paiementCaptor.capture());
        assertThat(paiementCaptor.getValue().datePaiement()).isEqualTo(datePaiementSaisi);
    }

    @Test
    void validate_should_throw_when_commande_not_draft() {
        commande.setStatut(CommandeVenteStatut.VALIDATE);

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        VenteValidateRequest validateReq = sampleValidateRequest();

        assertThatThrownBy(() -> service.validate(commandeId, validateReq))
                .isInstanceOf(BadArgumentException.class);

        verify(factureClientDomainService, never()).create(any());
        verify(commandeVenteDomainService, never()).validate(any());
    }

    @Test
    void validate_should_propagate_forbidden_when_not_owned() {
        when(commandeVenteDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Magasin foreignMagasin = new Magasin();
        foreignMagasin.setId(UUID.randomUUID());
        foreignMagasin.setEntreprise(other);
        commande.setMagasin(foreignMagasin);

        VenteValidateRequest validateReq = sampleValidateRequest();

        assertThatThrownBy(() -> service.validate(commandeId, validateReq))
                .isInstanceOf(ForbiddenException.class);

        verify(factureClientDomainService, never()).create(any());
    }

    @Test
    void updateLigne_should_update_quantite_and_prix() {
        LigneCommandeVente ligne = sampleLigne(100, new BigDecimal("10.00"));
        commande.setLignes(List.of(ligne));
        LigneVenteUpdateRequest req = new LigneVenteUpdateRequest(150, new BigDecimal("12.00"));

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(ligneCommandeVenteDomainService.findById(ligneId)).thenReturn(ligne);
        when(ligneCommandeVenteDomainService.update(eq(ligne), eq(150), eq(new BigDecimal("12.00"))))
                .thenAnswer(inv -> {
                    ligne.setQuantite(150);
                    ligne.setPrixUnitaire(new BigDecimal("12.00"));
                    ligne.setMontantTotal(new BigDecimal("1800.00"));
                    return ligne;
                });

        var response = service.updateLigne(commandeId, ligneId, req);

        assertThat(response.quantite()).isEqualTo(150);
        assertThat(response.prixUnitaire()).isEqualByComparingTo("12.00");
    }

    @Test
    void updateLigne_should_throw_when_prix_below_floor() {
        LigneCommandeVente ligne = sampleLigne(100, new BigDecimal("10.00"));
        commande.setLignes(List.of(ligne));
        LigneVenteUpdateRequest req = new LigneVenteUpdateRequest(150, new BigDecimal("5.00"));

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(ligneCommandeVenteDomainService.findById(ligneId)).thenReturn(ligne);

        assertThatThrownBy(() -> service.updateLigne(commandeId, ligneId, req))
                .isInstanceOf(BadArgumentException.class);

        verify(ligneCommandeVenteDomainService, never()).update(any(), anyInt(), any());
    }

    @Test
    void updateLigne_should_throw_when_commande_not_draft() {
        commande.setStatut(CommandeVenteStatut.VALIDATE);
        LigneVenteUpdateRequest req = new LigneVenteUpdateRequest(150, new BigDecimal("12.00"));

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThatThrownBy(() -> service.updateLigne(commandeId, ligneId, req))
                .isInstanceOf(BadArgumentException.class);

        verify(ligneCommandeVenteDomainService, never()).update(any(), anyInt(), any());
    }

    @Test
    void deleteLigne_should_remove_ligne_when_draft_and_not_last() {
        LigneCommandeVente ligne1 = sampleLigne(100, new BigDecimal("10.00"));
        LigneCommandeVente ligne2 = sampleLigne(50, new BigDecimal("12.00"));
        ligne2.setId(UUID.randomUUID());
        commande.setLignes(List.of(ligne1, ligne2));

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(ligneCommandeVenteDomainService.findById(ligneId)).thenReturn(ligne1);

        service.deleteLigne(commandeId, ligneId);

        verify(ligneCommandeVenteDomainService).delete(ligne1);
    }

    @Test
    void deleteLigne_should_throw_when_last_ligne() {
        LigneCommandeVente ligne = sampleLigne(100, new BigDecimal("10.00"));
        commande.setLignes(List.of(ligne));

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(ligneCommandeVenteDomainService.findById(ligneId)).thenReturn(ligne);

        assertThatThrownBy(() -> service.deleteLigne(commandeId, ligneId))
                .isInstanceOf(BadArgumentException.class);

        verify(ligneCommandeVenteDomainService, never()).delete(any());
    }

    @Test
    void deleteLigne_should_throw_when_commande_not_draft() {
        commande.setStatut(CommandeVenteStatut.VALIDATE);

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThatThrownBy(() -> service.deleteLigne(commandeId, ligneId))
                .isInstanceOf(BadArgumentException.class);

        verify(ligneCommandeVenteDomainService, never()).delete(any());
    }

    @Test
    void findDetailsById_should_return_commande_facture_lignes_paiements() {
        commande.setStatut(CommandeVenteStatut.VALIDATE);
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
    void findDetailsById_should_return_null_facture_when_draft() {
        commande.setCreatedBy(UUID.randomUUID().toString());

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(factureClientDomainService.findByCommandeId(commandeId)).thenReturn(Optional.empty());
        when(accountService.findUserSummaryByAccountId(commande.getCreatedBy()))
                .thenReturn(Optional.of(new UserSummaryResponse(employeId, "Diop Awa")));

        VenteDetailsResponse response = service.findDetailsById(commandeId);

        assertThat(response.facture()).isNull();
        assertThat(response.commande().statut()).isEqualTo(CommandeVenteStatut.DRAFT);

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

        UUID commandeIdLocal = commande.getId();

        when(commandeVenteDomainService.findById(commandeIdLocal)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThatThrownBy(() -> service.findDetailsById(commandeIdLocal))
                .isInstanceOf(ForbiddenException.class);

        verify(factureClientDomainService, never()).findByCommandeId(any());
    }

    private CommandeVente deliveredCommandeWithOneLigne() {
        UUID localLigneId = UUID.randomUUID();
        LigneCommandeVente ligne = new LigneCommandeVente();
        ligne.setId(localLigneId);
        commande.setStatut(CommandeVenteStatut.VALIDATE);
        commande.setCreatedAt(LocalDateTime.now().minusHours(1));
        commande.setLignes(List.of(ligne));
        return commande;
    }

    @Test
    void cancel_should_reinject_stock_and_mark_cancelled() {
        CommandeVente delivered = deliveredCommandeWithOneLigne();
        UUID localLigneId = delivered.getLignes().get(0).getId();

        EntreeStock lot = new EntreeStock();
        lot.setId(UUID.randomUUID());
        lot.setMagasin(magasin);
        lot.setProduit(produit);
        lot.setProductFournisseur(productFournisseur);
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
        when(sortieStockDomainService.findActiveByLigneVenteId(localLigneId)).thenReturn(List.of(sortie));
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, productFournisseurId)).thenReturn(Optional.of(stock));
        when(stockDomainService.creditQuantite(eq(stock), eq(8))).thenAnswer(inv -> {
            stock.setQuantiteDisponible(stock.getQuantiteDisponible() + 8);
            return stock;
        });
        when(commandeVenteDomainService.cancel(any(), any(), any())).thenAnswer(inv -> {
            CommandeVente arg = inv.getArgument(0);
            arg.setStatut(CommandeVenteStatut.CANCEL);
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
        assertThat(response.statut()).isEqualTo(CommandeVenteStatut.CANCEL);
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
        commande.setStatut(CommandeVenteStatut.CANCEL);
        commande.setCreatedAt(LocalDateTime.now().minusHours(1));

        UUID commandeIdLocal = commande.getId();

        when(commandeVenteDomainService.findById(commandeIdLocal)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        AnnulationVenteRequest req = new AnnulationVenteRequest("ERREUR_SAISIE", null);

        assertThatThrownBy(() -> service.cancel(commandeIdLocal, req))
                .isInstanceOf(BadArgumentException.class);

        verify(commandeVenteDomainService, never()).cancel(any(), any(), any());
        verify(stockDomainService, never()).creditQuantite(any(), anyInt());
    }

    @Test
    void cancel_should_throw_when_not_delivered() {
        commande.setStatut(CommandeVenteStatut.DRAFT);
        commande.setCreatedAt(LocalDateTime.now().minusHours(1));

        UUID commandeIdLocal = commande.getId();

        when(commandeVenteDomainService.findById(commandeIdLocal)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        AnnulationVenteRequest req = new AnnulationVenteRequest("ERREUR_SAISIE", null);

        assertThatThrownBy(() -> service.cancel(commandeIdLocal, req))
                .isInstanceOf(BadArgumentException.class);

        verify(commandeVenteDomainService, never()).cancel(any(), any(), any());
    }

    @Test
    void cancel_should_throw_when_window_expired() {
        commande.setStatut(CommandeVenteStatut.VALIDATE);
        commande.setCreatedAt(LocalDateTime.now().minusHours(48));

        UUID commandeIdLocal = commande.getId();

        when(commandeVenteDomainService.findById(commandeIdLocal)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(saleProperties.cancelWindowHours()).thenReturn(24);

        AnnulationVenteRequest req = new AnnulationVenteRequest("ERREUR_SAISIE", null);

        assertThatThrownBy(() -> service.cancel(commandeIdLocal, req))
                .isInstanceOf(BadArgumentException.class);

        verify(commandeVenteDomainService, never()).cancel(any(), any(), any());
    }

    @Test
    void cancel_should_throw_when_cross_entreprise() {
        commande.setStatut(CommandeVenteStatut.VALIDATE);
        commande.setCreatedAt(LocalDateTime.now().minusHours(1));

        UUID otherEntrepriseId = UUID.randomUUID();
        Entreprise other = new Entreprise();
        other.setId(otherEntrepriseId);
        Magasin foreignMagasin = new Magasin();
        foreignMagasin.setId(UUID.randomUUID());
        foreignMagasin.setEntreprise(other);
        commande.setMagasin(foreignMagasin);

        UUID commandeIdLocal = commande.getId();

        when(commandeVenteDomainService.findById(commandeIdLocal)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        AnnulationVenteRequest req = new AnnulationVenteRequest("ERREUR_SAISIE", null);

        assertThatThrownBy(() -> service.cancel(commandeIdLocal, req))
                .isInstanceOf(ForbiddenException.class);

        verify(commandeVenteDomainService, never()).cancel(any(), any(), any());
    }
}
