package org.store.achat.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.application.dto.AchatDraftResponse;
import org.store.achat.application.dto.AchatReceiveRequest;
import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatResponse;
import org.store.achat.application.dto.AnnulationAchatRequest;
import org.store.achat.application.dto.AnnulationAchatResponse;
import org.store.achat.application.dto.CommandeAchatCreate;
import org.store.achat.application.dto.FactureAchatCreate;
import org.store.achat.application.dto.FactureAchatCreateRequest;
import org.store.achat.application.dto.LigneAchatRequest;
import org.store.achat.application.dto.LigneAchatUpdateRequest;
import org.store.achat.application.dto.LigneCommandeAchatCreate;
import org.store.achat.application.dto.LigneCommandeAchatResponse;
import org.store.achat.application.dto.PaiementAchatCreate;
import org.store.achat.application.dto.PaiementAchatRequest;
import org.store.achat.application.service.impl.AchatServiceImpl;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.enums.MotifAnnulationAchat;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.achat.domain.enums.StatutFacture;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.model.FactureAchat;
import org.store.achat.domain.model.Fournisseur;
import org.store.achat.domain.model.LigneCommandeAchat;
import org.store.achat.domain.service.CommandeAchatDomainService;
import org.store.achat.domain.service.FactureAchatDomainService;
import org.store.achat.domain.service.LigneCommandeAchatDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.property.PurchaseProperties;
import org.store.achat.application.dto.LigneCommandeAchatUpdate;
import org.store.stock.application.dto.EntreeStockCreate;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.StockEntryContext;
import org.store.stock.application.service.IEntreeStockService;
import org.store.stock.application.service.IMouvementStockService;
import org.store.stock.application.service.IStockService;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.Stock;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchatServiceImplTest {

    @Mock private CommandeAchatDomainService commandeAchatDomainService;
    @Mock private LigneCommandeAchatDomainService ligneCommandeAchatDomainService;
    @Mock private FactureAchatDomainService factureAchatDomainService;
    @Mock private org.store.achat.domain.service.PaiementAchatDomainService paiementAchatDomainService;
    @Mock private IEntreeStockService entreeStockService;
    @Mock private IStockService stockService;
    @Mock private IMouvementStockService mouvementStockService;
    @Mock private IMagasinService magasinService;
    @Mock private IFournisseurService fournisseurService;
    @Mock private IProductFournisseurService productFournisseurService;
    @Mock private org.store.achat.application.service.ICommandeAchatService commandeAchatService;
    @Mock private ValidatorService validatorService;
    @Mock private PurchaseProperties purchaseProperties;
    @Mock private org.store.security.application.service.ICurrentUserService currentUserService;
    @Mock private org.store.audit.application.service.IAuditEventPublisher auditEventPublisher;
    @Mock private IMoyenPaiementService moyenPaiementService;
    @Mock private org.store.common.service.IUploadFileService uploadFileService;
    @Mock private org.store.sequence.application.service.IDocumentSequenceService documentSequenceService;

    private static final UUID MOYEN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private org.store.paiement.domain.model.MoyenPaiement moyenCash() {
        org.store.paiement.domain.model.MoyenPaiement m = new org.store.paiement.domain.model.MoyenPaiement();
        m.setId(MOYEN_ID); m.setLibelle("Espèces"); m.setCode("CASH");
        return m;
    }

    @InjectMocks
    private AchatServiceImpl service;

    private UUID magasinId;
    private UUID fournisseurId;
    private UUID productFournisseurId;
    private UUID commandeId;
    private UUID ligneId;
    private Entreprise entreprise;
    private Magasin magasin;
    private Fournisseur fournisseur;
    private Product produit;
    private ProductFournisseur productFournisseur;
    private CommandeAchat commande;
    private FactureAchat facture;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserService.getCurrent()).thenReturn(new org.store.security.application.dto.UserPrincipal(java.util.UUID.randomUUID(), null, java.util.UUID.randomUUID(), null, "test", null, null, "OWNER", java.util.List.of()));
        magasinId = UUID.randomUUID();
        fournisseurId = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();
        commandeId = UUID.randomUUID();
        ligneId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(UUID.randomUUID());

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setNom("Magasin Central");
        magasin.setEntreprise(entreprise);

        fournisseur = new Fournisseur();
        fournisseur.setId(fournisseurId);
        fournisseur.setNom("Fournisseur Chine");
        fournisseur.setEntreprise(entreprise);

        produit = new Product();
        produit.setId(UUID.randomUUID());
        produit.setEntreprise(entreprise);

        org.store.produit.domain.model.Quality quality = new org.store.produit.domain.model.Quality();
        quality.setId(UUID.randomUUID());
        quality.setLibelle("Original");

        productFournisseur = new ProductFournisseur();
        productFournisseur.setId(productFournisseurId);
        productFournisseur.setProduct(produit);
        productFournisseur.setFournisseur(fournisseur);
        productFournisseur.setQuality(quality);
        productFournisseur.setPrixAchat(new BigDecimal("10.00"));

        commande = new CommandeAchat();
        commande.setId(commandeId);
        commande.setReference("CMD-AUTO");
        commande.setStatut(CommandeAchatStatut.DRAFT);
        commande.setMagasin(magasin);
        commande.setFournisseur(fournisseur);
        commande.setLignes(new ArrayList<>());

        facture = new FactureAchat();
        facture.setId(UUID.randomUUID());
        facture.setCommande(commande);
        facture.setNumero("FAC-001");
        facture.setMontantTotal(new BigDecimal("1000.00"));
        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setStatut(StatutFacture.NON_PAYEE);
    }

    private AchatRequest sampleRequest() {
        return new AchatRequest(
                magasinId, fournisseurId, LocalDate.of(2026, 5, 15),
                List.of(new LigneAchatRequest(produit.getId(), productFournisseur.getQuality().getId(), 100, new BigDecimal("10.00"), new BigDecimal("15.00"), "LOT-1", null))
        );
    }

    private AchatReceiveRequest sampleReceiveRequest() {
        return new AchatReceiveRequest(
                new FactureAchatCreateRequest("FAC-001", LocalDate.of(2026, 5, 15), LocalDate.of(2026, 6, 15)),
                null);
    }

    private LigneCommandeAchat sampleLigne(int quantite, BigDecimal prixAchat, BigDecimal prixVente) {
        LigneCommandeAchat ligne = new LigneCommandeAchat();
        ligne.setId(ligneId);
        ligne.setCommande(commande);
        ligne.setProductFournisseur(productFournisseur);
        ligne.setQuantite(quantite);
        ligne.setPrixAchat(prixAchat);
        ligne.setPrixVente(prixVente);
        ligne.setNumeroLot("LOT-1");
        return ligne;
    }

    @Test
    void create_should_persist_draft_without_stock_or_facture() {
        AchatRequest req = sampleRequest();

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(fournisseurService.findById(fournisseurId)).thenReturn(fournisseur);
        when(fournisseurService.ensureBelongsToCurrentEntreprise(fournisseur)).thenReturn(fournisseur);
        when(productFournisseurService.findOrCreate(any())).thenReturn(new org.store.produit.application.dto.ProductFournisseurResponse(productFournisseur));
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(documentSequenceService.generateReference(any(), eq(org.store.sequence.domain.enums.TypeDocument.COMMANDE_ACHAT))).thenReturn("CMD-AUTO");
        when(commandeAchatDomainService.create(any(CommandeAchatCreate.class))).thenReturn(commande);
        when(commandeAchatDomainService.findById(commande.getId())).thenReturn(commande);
        LigneCommandeAchat mockLigne = new LigneCommandeAchat();
        mockLigne.setId(ligneId);
        mockLigne.setProductFournisseur(productFournisseur);
        mockLigne.setQuantite(5);
        mockLigne.setPrixAchat(new BigDecimal("10.00"));
        mockLigne.setPrixVente(new BigDecimal("15.00"));
        when(ligneCommandeAchatDomainService.create(any(LigneCommandeAchatCreate.class))).thenReturn(mockLigne);

        AchatDraftResponse response = service.create(req);

        assertThat(response.commande().statut()).isEqualTo(CommandeAchatStatut.DRAFT);

        ArgumentCaptor<CommandeAchatCreate> captor = ArgumentCaptor.forClass(CommandeAchatCreate.class);
        verify(commandeAchatDomainService).create(captor.capture());
        assertThat(captor.getValue().statut()).isEqualTo(CommandeAchatStatut.DRAFT);

        verify(factureAchatDomainService, never()).create(any());
        verify(entreeStockService, never()).createEntreeStock(any());
        verify(mouvementStockService, never()).journalize(any(), any());
        verify(productFournisseurService, never()).applyPrixVenteFromPurchase(any(), any());
    }


    @Test
    void create_should_propagate_forbidden_when_magasin_not_accessible() {
        AchatRequest req = sampleRequest();

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void receive_should_create_facture_and_materialize_stock_for_every_ligne() {
        LigneCommandeAchat ligne = sampleLigne(100, new BigDecimal("10.00"), new BigDecimal("15.00"));
        commande.setLignes(List.of(ligne));
        Stock stockAfter = new Stock();
        stockAfter.setQuantiteDisponible(100);

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(factureAchatDomainService.create(any(FactureAchatCreate.class))).thenReturn(facture);
        when(stockService.findByMagasinAndProductFournisseur(magasinId, productFournisseur.getId())).thenReturn(Optional.empty());
        when(entreeStockService.createEntreeStock(any(EntreeStockCreate.class))).thenReturn(new EntreeStock());
        when(stockService.createOrUpdateEntry(any(StockEntryContext.class))).thenReturn(stockAfter);
        when(commandeAchatDomainService.markReceptionnee(commande)).thenAnswer(inv -> {
            commande.setStatut(CommandeAchatStatut.RECEPTIONNEE);
            return commande;
        });

        AchatResponse response = service.receive(commandeId, sampleReceiveRequest());

        assertThat(response.commande().statut()).isEqualTo(CommandeAchatStatut.RECEPTIONNEE);
        assertThat(response.facture().numero()).isEqualTo("FAC-001");

        ArgumentCaptor<FactureAchatCreate> factureCaptor = ArgumentCaptor.forClass(FactureAchatCreate.class);
        verify(factureAchatDomainService).create(factureCaptor.capture());
        assertThat(factureCaptor.getValue().montantTotal()).isEqualByComparingTo(new BigDecimal("1000.00"));

        ArgumentCaptor<EntreeStockCreate> entreeCaptor = ArgumentCaptor.forClass(EntreeStockCreate.class);
        verify(entreeStockService).createEntreeStock(entreeCaptor.capture());
        assertThat(entreeCaptor.getValue().quantite()).isEqualTo(100);
        assertThat(entreeCaptor.getValue().numeroLot()).isEqualTo("LOT-1");
        assertThat(entreeCaptor.getValue().commandeAchat()).isEqualTo(commande);

        ArgumentCaptor<MouvementJournalize> mouvementCaptor = ArgumentCaptor.forClass(MouvementJournalize.class);
        verify(mouvementStockService).journalize(eq(stockAfter), mouvementCaptor.capture());
        assertThat(mouvementCaptor.getValue().type()).isEqualTo(MouvementStockType.ENTREE_ACHAT);
        assertThat(mouvementCaptor.getValue().quantite()).isEqualTo(100);
        assertThat(mouvementCaptor.getValue().referenceDocument()).isEqualTo("CMD-AUTO");

        verify(productFournisseurService).applyPrixVenteFromPurchase(productFournisseur, new BigDecimal("15.00"));
        verify(commandeAchatDomainService).markReceptionnee(commande);
    }

    @Test
    void receive_should_compute_total_from_lines() {
        LigneCommandeAchat l1 = sampleLigne(100, new BigDecimal("10.00"), new BigDecimal("15.00"));
        LigneCommandeAchat l2 = sampleLigne(50, new BigDecimal("15.00"), new BigDecimal("20.00"));
        l2.setId(UUID.randomUUID());
        commande.setLignes(List.of(l1, l2));
        Stock stockAfter = new Stock();
        stockAfter.setQuantiteDisponible(150);

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(factureAchatDomainService.create(any(FactureAchatCreate.class))).thenReturn(facture);
        when(stockService.findByMagasinAndProductFournisseur(magasinId, productFournisseur.getId())).thenReturn(Optional.empty());
        when(entreeStockService.createEntreeStock(any(EntreeStockCreate.class))).thenReturn(new EntreeStock());
        when(stockService.createOrUpdateEntry(any(StockEntryContext.class))).thenReturn(stockAfter);
        when(commandeAchatDomainService.markReceptionnee(commande)).thenReturn(commande);

        service.receive(commandeId, sampleReceiveRequest());

        ArgumentCaptor<FactureAchatCreate> captor = ArgumentCaptor.forClass(FactureAchatCreate.class);
        verify(factureAchatDomainService).create(captor.capture());
        // 100 × 10.00 + 50 × 15.00 = 1000 + 750 = 1750
        assertThat(captor.getValue().montantTotal()).isEqualByComparingTo(new BigDecimal("1750.00"));
        verify(entreeStockService, org.mockito.Mockito.times(2)).createEntreeStock(any(EntreeStockCreate.class));
    }

    @Test
    void receive_should_persist_initial_paiement_when_provided() {
        LigneCommandeAchat ligne = sampleLigne(100, new BigDecimal("10.00"), new BigDecimal("15.00"));
        commande.setLignes(List.of(ligne));
        Stock stockAfter = new Stock();
        stockAfter.setQuantiteDisponible(100);

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(factureAchatDomainService.create(any(FactureAchatCreate.class))).thenReturn(facture);
        when(factureAchatDomainService.applyPaiement(eq(facture), eq(new BigDecimal("400.00")))).thenReturn(facture);
        when(stockService.findByMagasinAndProductFournisseur(any(), any())).thenReturn(Optional.empty());
        when(entreeStockService.createEntreeStock(any(EntreeStockCreate.class))).thenReturn(new EntreeStock());
        when(stockService.createOrUpdateEntry(any(StockEntryContext.class))).thenReturn(stockAfter);
        when(commandeAchatDomainService.markReceptionnee(commande)).thenReturn(commande);

        AchatReceiveRequest body = new AchatReceiveRequest(
                new FactureAchatCreateRequest("FAC-001", LocalDate.of(2026, 5, 15), LocalDate.of(2026, 6, 15)),
                new PaiementAchatRequest(new BigDecimal("400.00"), LocalDate.of(2026, 5, 15), MOYEN_ID));
        when(moyenPaiementService.findById(MOYEN_ID)).thenReturn(moyenCash());

        service.receive(commandeId, body);

        verify(paiementAchatDomainService).create(any(PaiementAchatCreate.class));
        verify(factureAchatDomainService).applyPaiement(facture, new BigDecimal("400.00"));
    }

    @Test
    void receive_should_throw_when_paiement_exceeds_total() {
        LigneCommandeAchat ligne = sampleLigne(100, new BigDecimal("10.00"), new BigDecimal("15.00"));
        commande.setLignes(List.of(ligne));

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(factureAchatDomainService.create(any(FactureAchatCreate.class))).thenReturn(facture);

        AchatReceiveRequest body = new AchatReceiveRequest(
                new FactureAchatCreateRequest("FAC-001", LocalDate.of(2026, 5, 15), LocalDate.of(2026, 6, 15)),
                new PaiementAchatRequest(new BigDecimal("2000.00"), LocalDate.of(2026, 5, 15), MOYEN_ID));

        assertThatThrownBy(() -> service.receive(commandeId, body))
                .isInstanceOf(BadArgumentException.class)
                .hasMessageContaining("exceedsRemaining");

        verify(paiementAchatDomainService, never()).create(any());
        verify(entreeStockService, never()).createEntreeStock(any());
        verify(commandeAchatDomainService, never()).markReceptionnee(any());
    }

    @Test
    void receive_should_throw_when_commande_not_draft() {
        commande.setStatut(CommandeAchatStatut.RECEPTIONNEE);

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);

        AchatReceiveRequest receiveReq = sampleReceiveRequest();

        assertThatThrownBy(() -> service.receive(commandeId, receiveReq))
                .isInstanceOf(BadArgumentException.class)
                .hasMessageContaining("notDraft");

        verify(factureAchatDomainService, never()).create(any());
        verify(entreeStockService, never()).createEntreeStock(any());
        verify(commandeAchatDomainService, never()).markReceptionnee(any());
    }

    @Test
    void receive_should_propagate_forbidden_when_not_owned() {
        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande))
                .thenThrow(new ForbiddenException("commandeAchat.notOwned"));

        AchatReceiveRequest receiveReq = sampleReceiveRequest();

        assertThatThrownBy(() -> service.receive(commandeId, receiveReq))
                .isInstanceOf(ForbiddenException.class);

        verify(factureAchatDomainService, never()).create(any());
    }

    @Test
    void updateLigne_should_update_quantite_and_prices() {
        LigneCommandeAchat ligne = sampleLigne(100, new BigDecimal("10.00"), new BigDecimal("15.00"));
        commande.setLignes(List.of(ligne));
        LigneAchatUpdateRequest req = new LigneAchatUpdateRequest(150, new BigDecimal("12.00"), new BigDecimal("18.00"), "LOT-2", null);

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(ligneCommandeAchatDomainService.findById(ligneId)).thenReturn(ligne);
        when(ligneCommandeAchatDomainService.update(eq(ligne), any(LigneCommandeAchatUpdate.class)))
                .thenAnswer(inv -> {
                    ligne.setQuantite(150);
                    ligne.setPrixAchat(new BigDecimal("12.00"));
                    ligne.setPrixVente(new BigDecimal("18.00"));
                    ligne.setNumeroLot("LOT-2");
                    return ligne;
                });

        LigneCommandeAchatResponse response = service.updateLigne(commandeId, ligneId, req);

        assertThat(response.quantite()).isEqualTo(150);
        assertThat(response.prixAchat()).isEqualByComparingTo("12.00");
        assertThat(response.prixVente()).isEqualByComparingTo("18.00");
        verify(productFournisseurService).ensurePrixVenteGreaterThanPrixAchat(new BigDecimal("18.00"), new BigDecimal("12.00"));
    }

    @Test
    void updateLigne_should_throw_when_commande_not_draft() {
        commande.setStatut(CommandeAchatStatut.RECEPTIONNEE);
        LigneAchatUpdateRequest req = new LigneAchatUpdateRequest(150, new BigDecimal("12.00"), new BigDecimal("18.00"), null, null);

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);

        assertThatThrownBy(() -> service.updateLigne(commandeId, ligneId, req))
                .isInstanceOf(BadArgumentException.class);

        verify(ligneCommandeAchatDomainService, never()).update(any(), any(LigneCommandeAchatUpdate.class));
    }

    @Test
    void updateLigne_should_throw_when_ligne_belongs_to_other_commande() {
        CommandeAchat autreCommande = new CommandeAchat();
        autreCommande.setId(UUID.randomUUID());
        LigneCommandeAchat ligne = sampleLigne(100, new BigDecimal("10.00"), new BigDecimal("15.00"));
        ligne.setCommande(autreCommande);
        LigneAchatUpdateRequest req = new LigneAchatUpdateRequest(150, new BigDecimal("12.00"), new BigDecimal("18.00"), null, null);

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(ligneCommandeAchatDomainService.findById(ligneId)).thenReturn(ligne);

        assertThatThrownBy(() -> service.updateLigne(commandeId, ligneId, req))
                .isInstanceOf(BadArgumentException.class);

        verify(ligneCommandeAchatDomainService, never()).update(any(), any(LigneCommandeAchatUpdate.class));
    }

    @Test
    void deleteLigne_should_remove_ligne_when_draft_and_not_last() {
        LigneCommandeAchat ligne1 = sampleLigne(100, new BigDecimal("10.00"), new BigDecimal("15.00"));
        LigneCommandeAchat ligne2 = sampleLigne(50, new BigDecimal("12.00"), new BigDecimal("18.00"));
        ligne2.setId(UUID.randomUUID());
        commande.setLignes(new ArrayList<>(List.of(ligne1, ligne2)));

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(ligneCommandeAchatDomainService.findById(ligneId)).thenReturn(ligne1);

        service.deleteLigne(commandeId, ligneId);

        verify(ligneCommandeAchatDomainService).delete(ligne1);
    }

    @Test
    void deleteLigne_should_throw_when_last_ligne() {
        LigneCommandeAchat ligne = sampleLigne(100, new BigDecimal("10.00"), new BigDecimal("15.00"));
        commande.setLignes(List.of(ligne));

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(ligneCommandeAchatDomainService.findById(ligneId)).thenReturn(ligne);

        assertThatThrownBy(() -> service.deleteLigne(commandeId, ligneId))
                .isInstanceOf(BadArgumentException.class);

        verify(ligneCommandeAchatDomainService, never()).delete(any());
    }

    @Test
    void deleteLigne_should_throw_when_commande_not_draft() {
        commande.setStatut(CommandeAchatStatut.RECEPTIONNEE);

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);

        assertThatThrownBy(() -> service.deleteLigne(commandeId, ligneId))
                .isInstanceOf(BadArgumentException.class);

        verify(ligneCommandeAchatDomainService, never()).delete(any());
    }

    @Test
    void deleteDraft_should_delete_commande_when_draft() {
        commande.setStatut(CommandeAchatStatut.DRAFT);

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);

        service.deleteDraft(commandeId);

        verify(commandeAchatDomainService).delete(commande);
    }

    @Test
    void deleteDraft_should_throw_when_commande_not_draft() {
        commande.setStatut(CommandeAchatStatut.RECEPTIONNEE);

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);

        assertThatThrownBy(() -> service.deleteDraft(commandeId))
                .isInstanceOf(BadArgumentException.class)
                .hasMessageContaining("notDraft");

        verify(commandeAchatDomainService, never()).delete(any());
    }

    @Test
    void deleteDraft_should_propagate_forbidden_when_not_owned() {
        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande))
                .thenThrow(new ForbiddenException("commandeAchat.notOwned"));

        assertThatThrownBy(() -> service.deleteDraft(commandeId))
                .isInstanceOf(ForbiddenException.class);

        verify(commandeAchatDomainService, never()).delete(any());
    }

    @Test
    void findDetailsById_should_return_commande_facture_and_lignes() {
        LigneCommandeAchat ligne = sampleLigne(10, new BigDecimal("10.00"), new BigDecimal("15.00"));
        commande.setLignes(List.of(ligne));

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(factureAchatDomainService.findByCommandeId(commandeId)).thenReturn(Optional.of(facture));

        org.store.achat.application.dto.AchatDetailsResponse response = service.findDetailsById(commandeId);

        assertThat(response.facture().numero()).isEqualTo("FAC-001");
        assertThat(response.lignes()).hasSize(1);
    }

    @Test
    void findDetailsById_should_return_null_facture_when_draft() {
        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(factureAchatDomainService.findByCommandeId(commandeId)).thenReturn(Optional.empty());

        org.store.achat.application.dto.AchatDetailsResponse response = service.findDetailsById(commandeId);

        assertThat(response.facture()).isNull();
        assertThat(response.commande().statut()).isEqualTo(CommandeAchatStatut.DRAFT);
    }

    @Test
    void findDetailsById_should_throw_when_commande_not_owned() {
        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande))
                .thenThrow(new ForbiddenException("commandeAchat.notOwned"));

        assertThatThrownBy(() -> service.findDetailsById(commandeId))
                .isInstanceOf(ForbiddenException.class);

        verify(factureAchatDomainService, never()).findByCommandeId(any());
    }

    private EntreeStock sampleLot(int quantiteInitiale, int quantiteRestante) {
        EntreeStock lot = new EntreeStock();
        lot.setId(UUID.randomUUID());
        lot.setMagasin(magasin);
        lot.setProduit(produit);
        lot.setProductFournisseur(productFournisseur);
        lot.setQuantiteInitiale(quantiteInitiale);
        lot.setQuantiteRestante(quantiteRestante);
        lot.setPrixAchat(new BigDecimal("10.00"));
        lot.setCommandeAchat(commande);
        return lot;
    }

    private void prepareReceptionneeCommande() {
        commande.setStatut(CommandeAchatStatut.RECEPTIONNEE);
        commande.setCreatedAt(LocalDateTime.now().minusHours(1));
    }

    @Test
    void cancel_should_withdraw_stock_and_switch_status_when_nominal() {
        prepareReceptionneeCommande();
        EntreeStock lot = sampleLot(100, 100);
        Stock stock = new Stock();
        stock.setQuantiteDisponible(100);

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(purchaseProperties.cancelWindowHours()).thenReturn(24);
        when(entreeStockService.findByCommandeAchatId(commandeId)).thenReturn(List.of(lot));
        when(stockService.findByMagasinAndProductFournisseur(magasinId, productFournisseur.getId())).thenReturn(Optional.of(stock));
        when(stockService.decrement(stock, 100)).thenAnswer(inv -> {
            stock.setQuantiteDisponible(0);
            return stock;
        });
        when(commandeAchatDomainService.cancel(eq(commande), eq(MotifAnnulationAchat.ERREUR_SAISIE), any())).thenAnswer(inv -> {
            commande.setStatut(CommandeAchatStatut.ANNULEE);
            commande.setMotifAnnulation(MotifAnnulationAchat.ERREUR_SAISIE);
            commande.setDateAnnulation(LocalDateTime.now());
            return commande;
        });
        when(factureAchatDomainService.findByCommandeId(commandeId)).thenReturn(Optional.of(facture));

        AnnulationAchatResponse response = service.cancel(commandeId, new AnnulationAchatRequest("ERREUR_SAISIE", "Saisie erronée"));

        assertThat(response.statut()).isEqualTo(CommandeAchatStatut.ANNULEE);
        assertThat(response.motif()).isEqualTo(MotifAnnulationAchat.ERREUR_SAISIE);
        assertThat(response.totalQuantiteRetiree()).isEqualTo(100);
        assertThat(response.nombreMouvementsCrees()).isEqualTo(1);

        verify(entreeStockService).markAsAnnulee(lot);
        verify(factureAchatDomainService).cancel(facture);

        ArgumentCaptor<MouvementJournalize> mouvementCaptor = ArgumentCaptor.forClass(MouvementJournalize.class);
        verify(mouvementStockService).journalize(eq(stock), mouvementCaptor.capture());
        assertThat(mouvementCaptor.getValue().type()).isEqualTo(MouvementStockType.RETOUR_FOURNISSEUR);
        assertThat(mouvementCaptor.getValue().quantite()).isEqualTo(100);
        assertThat(mouvementCaptor.getValue().referenceDocument()).isEqualTo("CMD-AUTO");
    }

    @Test
    void cancel_should_throw_when_already_cancelled() {
        commande.setStatut(CommandeAchatStatut.ANNULEE);
        commande.setCreatedAt(LocalDateTime.now().minusHours(1));

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);

        AnnulationAchatRequest cancelReq = new AnnulationAchatRequest("ERREUR_SAISIE", null);

        assertThatThrownBy(() -> service.cancel(commandeId, cancelReq))
                .isInstanceOf(BadArgumentException.class)
                .hasMessageContaining("alreadyCancelled");

        verify(entreeStockService, never()).findByCommandeAchatId(any());
        verify(commandeAchatDomainService, never()).cancel(any(), any(), any());
    }

    @Test
    void cancel_should_throw_when_statut_is_draft() {
        commande.setStatut(CommandeAchatStatut.DRAFT);
        commande.setCreatedAt(LocalDateTime.now().minusHours(1));

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);

        AnnulationAchatRequest cancelReq = new AnnulationAchatRequest("ERREUR_SAISIE", null);

        assertThatThrownBy(() -> service.cancel(commandeId, cancelReq))
                .isInstanceOf(BadArgumentException.class)
                .hasMessageContaining("notCancellable");

        verify(commandeAchatDomainService, never()).cancel(any(), any(), any());
    }

    @Test
    void cancel_should_throw_when_cancel_window_expired() {
        commande.setStatut(CommandeAchatStatut.RECEPTIONNEE);
        commande.setCreatedAt(LocalDateTime.now().minusHours(48));

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(purchaseProperties.cancelWindowHours()).thenReturn(24);

        AnnulationAchatRequest cancelReq = new AnnulationAchatRequest("ERREUR_SAISIE", null);

        assertThatThrownBy(() -> service.cancel(commandeId, cancelReq))
                .isInstanceOf(BadArgumentException.class)
                .hasMessageContaining("windowExpired");

        verify(entreeStockService, never()).findByCommandeAchatId(any());
    }

    @Test
    void cancel_should_propagate_forbidden_when_not_owned() {
        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande))
                .thenThrow(new ForbiddenException("commandeAchat.notOwned"));

        AnnulationAchatRequest cancelReq = new AnnulationAchatRequest("ERREUR_SAISIE", null);

        assertThatThrownBy(() -> service.cancel(commandeId, cancelReq))
                .isInstanceOf(ForbiddenException.class);

        verify(entreeStockService, never()).findByCommandeAchatId(any());
        verify(commandeAchatDomainService, never()).cancel(any(), any(), any());
    }

    @Test
    void cancel_should_throw_when_at_least_one_lot_already_consumed() {
        prepareReceptionneeCommande();
        EntreeStock lotIntact = sampleLot(100, 100);
        EntreeStock lotConsomme = sampleLot(50, 30);

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(purchaseProperties.cancelWindowHours()).thenReturn(24);
        when(entreeStockService.findByCommandeAchatId(commandeId)).thenReturn(List.of(lotIntact, lotConsomme));

        AnnulationAchatRequest cancelReq = new AnnulationAchatRequest("ERREUR_SAISIE", null);

        assertThatThrownBy(() -> service.cancel(commandeId, cancelReq))
                .isInstanceOf(BadArgumentException.class)
                .hasMessageContaining("lotAlreadyConsumed");

        verify(stockService, never()).decrement(any(), any(int.class));
        verify(entreeStockService, never()).markAsAnnulee(any());
        verify(commandeAchatDomainService, never()).cancel(any(), any(), any());
    }

    @Test
    void cancel_should_throw_when_facture_has_paiement() {
        prepareReceptionneeCommande();
        facture.setMontantPaye(new BigDecimal("250.00"));

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(purchaseProperties.cancelWindowHours()).thenReturn(24);
        when(factureAchatDomainService.findByCommandeId(commandeId)).thenReturn(Optional.of(facture));

        AnnulationAchatRequest cancelReq = new AnnulationAchatRequest("ERREUR_SAISIE", null);

        assertThatThrownBy(() -> service.cancel(commandeId, cancelReq))
                .isInstanceOf(BadArgumentException.class)
                .hasMessageContaining("hasPaiements");

        verify(entreeStockService, never()).findByCommandeAchatId(any());
        verify(commandeAchatDomainService, never()).cancel(any(), any(), any());
    }
}
