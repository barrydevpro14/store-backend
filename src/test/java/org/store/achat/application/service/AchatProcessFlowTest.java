package org.store.achat.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.application.dto.AchatDraftResponse;
import org.store.achat.application.dto.AchatReceiveRequest;
import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatResponse;
import org.store.achat.application.dto.AnnulationAchatRequest;
import org.store.achat.application.dto.AnnulationAchatResponse;
import org.store.achat.application.dto.FactureAchatCreateRequest;
import org.store.achat.application.dto.LigneAchatRequest;
import org.store.achat.application.dto.LigneAchatUpdateRequest;
import org.store.achat.application.dto.LigneCommandeAchatResponse;
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
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.property.PurchaseProperties;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.EntreeStockDomainService;
import org.store.stock.domain.service.MouvementStockDomainService;
import org.store.stock.domain.service.StockDomainService;

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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * End-to-end process tests for the purchase lifecycle.
 *
 * Covers the complete sequence: DRAFT → (edit lines) → RECEIVE → CANCEL
 * and the discard path: DRAFT → DELETE
 *
 * Each test exercises multiple service calls in sequence and verifies
 * both the returned state and the side-effects (stock, facture, movements).
 */
@ExtendWith(MockitoExtension.class)
class AchatProcessFlowTest {

    @Mock private CommandeAchatDomainService commandeAchatDomainService;
    @Mock private LigneCommandeAchatDomainService ligneCommandeAchatDomainService;
    @Mock private FactureAchatDomainService factureAchatDomainService;
    @Mock private org.store.achat.domain.service.PaiementAchatDomainService paiementAchatDomainService;
    @Mock private EntreeStockDomainService entreeStockDomainService;
    @Mock private StockDomainService stockDomainService;
    @Mock private MouvementStockDomainService mouvementStockDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private IFournisseurService fournisseurService;
    @Mock private IProductFournisseurService productFournisseurService;
    @Mock private org.store.achat.application.service.ICommandeAchatService commandeAchatService;
    @Mock private ValidatorService validatorService;
    @Mock private PurchaseProperties purchaseProperties;
    @Mock private org.store.security.application.service.ICurrentUserService currentUserService;
    @Mock private org.store.audit.application.service.IAuditEventPublisher auditEventPublisher;
    @Mock private IMoyenPaiementService moyenPaiementService;

    private static final UUID MOYEN_WAVE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private org.store.paiement.domain.model.MoyenPaiement moyenWave() {
        org.store.paiement.domain.model.MoyenPaiement m = new org.store.paiement.domain.model.MoyenPaiement();
        m.setId(MOYEN_WAVE_ID); m.setLibelle("Wave"); m.setCode("WAVE");
        return m;
    }

    @InjectMocks
    private AchatServiceImpl service;

    private UUID magasinId, fournisseurId, productFournisseurId, commandeId, ligneId;
    private Entreprise entreprise;
    private Magasin magasin;
    private Fournisseur fournisseur;
    private ProductFournisseur productFournisseur;
    private CommandeAchat draftCommande;
    private FactureAchat facture;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserService.getCurrent()).thenReturn(
                new org.store.security.application.dto.UserPrincipal(
                        UUID.randomUUID(), null, UUID.randomUUID(), null,
                        "manager", null, null, "OWNER", List.of()));

        magasinId           = UUID.randomUUID();
        fournisseurId       = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();
        commandeId          = UUID.randomUUID();
        ligneId             = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(UUID.randomUUID());

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setNom("Magasin Test");
        magasin.setEntreprise(entreprise);

        fournisseur = new Fournisseur();
        fournisseur.setId(fournisseurId);
        fournisseur.setNom("Fournisseur Test");
        fournisseur.setEntreprise(entreprise);

        Product produit = new Product();
        produit.setId(UUID.randomUUID());
        produit.setNom("Moteur");
        produit.setEntreprise(entreprise);

        org.store.produit.domain.model.Quality quality = new org.store.produit.domain.model.Quality();
        quality.setId(java.util.UUID.randomUUID());
        quality.setLibelle("Original");

        productFournisseur = new ProductFournisseur();
        productFournisseur.setId(productFournisseurId);
        productFournisseur.setProduct(produit);
        productFournisseur.setFournisseur(fournisseur);
        productFournisseur.setQuality(quality);
        productFournisseur.setPrixAchat(new BigDecimal("50000.00"));
        productFournisseur.setPrixVente(new BigDecimal("75000.00"));

        draftCommande = new CommandeAchat();
        draftCommande.setId(commandeId);
        draftCommande.setReference("CMD-TEST-001");
        draftCommande.setStatut(CommandeAchatStatut.DRAFT);
        draftCommande.setMagasin(magasin);
        draftCommande.setFournisseur(fournisseur);
        draftCommande.setDate(LocalDate.of(2026, 6, 3));
        draftCommande.setCreatedAt(LocalDateTime.now());
        draftCommande.setLignes(new ArrayList<>());

        facture = new FactureAchat();
        facture.setId(UUID.randomUUID());
        facture.setNumero("FACT-2026-001");
        facture.setMontantTotal(new BigDecimal("500000.00"));
        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setStatut(StatutFacture.NON_PAYEE);
        facture.setDate(LocalDate.of(2026, 6, 3));
        facture.setDateEcheance(LocalDate.of(2026, 7, 3));
        facture.setCommande(draftCommande);
    }

    // ── Scenario 1: Happy path — CREATE DRAFT → RECEIVE ─────────────────────

    @Test
    void process_create_draft_then_receive_produces_receptionnee_commande_with_facture_and_stock() {
        // ── STEP 1: Create DRAFT ─────────────────────────────────────────────
        AchatRequest createRequest = new AchatRequest(
                magasinId, fournisseurId, LocalDate.of(2026, 6, 3),
                List.of(new LigneAchatRequest(productFournisseur.getProduct().getId(), productFournisseur.getQuality().getId(), 10,
                        new BigDecimal("50000.00"), new BigDecimal("75000.00"), "LOT-001", null)));

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(fournisseurService.findById(fournisseurId)).thenReturn(fournisseur);
        when(fournisseurService.ensureBelongsToCurrentEntreprise(fournisseur)).thenReturn(fournisseur);
        when(productFournisseurService.findOrCreate(any())).thenReturn(new org.store.produit.application.dto.ProductFournisseurResponse(productFournisseur));
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(commandeAchatDomainService.create(any())).thenReturn(draftCommande);
        when(commandeAchatDomainService.findById(commandeId)).thenReturn(draftCommande);
        when(ligneCommandeAchatDomainService.create(any())).thenAnswer(inv -> {
            LigneCommandeAchat l = new LigneCommandeAchat();
            l.setId(ligneId);
            l.setCommande(draftCommande);
            l.setProductFournisseur(productFournisseur);
            l.setQuantite(10);
            l.setPrixAchat(new BigDecimal("50000.00"));
            l.setPrixVente(new BigDecimal("75000.00"));
            draftCommande.getLignes().add(l);
            return l;
        });

        AchatDraftResponse draftResult = service.create(createRequest);

        assertThat(draftResult.commande().statut()).isEqualTo(CommandeAchatStatut.DRAFT);

        // ── STEP 2: Receive → RECEPTIONNEE ───────────────────────────────────
        CommandeAchat receptionneeCommande = new CommandeAchat();
        receptionneeCommande.setId(commandeId);
        receptionneeCommande.setReference("CMD-TEST-001");
        receptionneeCommande.setStatut(CommandeAchatStatut.RECEPTIONNEE);
        receptionneeCommande.setMagasin(magasin);
        receptionneeCommande.setFournisseur(fournisseur);
        receptionneeCommande.setLignes(draftCommande.getLignes());

        Stock stock = new Stock();
        stock.setMagasin(magasin);
        stock.setProductFournisseur(productFournisseur);
        stock.setQuantiteDisponible(10);
        stock.setPrixAchatMoyen(new BigDecimal("50000.00"));

        AchatReceiveRequest receiveRequest = new AchatReceiveRequest(
                new FactureAchatCreateRequest("FACT-2026-001",
                        LocalDate.of(2026, 6, 3), LocalDate.of(2026, 7, 3)),
                null);

        when(commandeAchatService.findById(commandeId)).thenReturn(draftCommande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(draftCommande)).thenReturn(draftCommande);
        when(factureAchatDomainService.create(any())).thenReturn(facture);
        when(entreeStockDomainService.create(any())).thenReturn(new EntreeStock());
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(any(), any())).thenReturn(Optional.empty());
        when(stockDomainService.createOrUpdateEntry(any())).thenReturn(stock);
        when(commandeAchatDomainService.markReceptionnee(draftCommande)).thenReturn(receptionneeCommande);

        AchatResponse receiveResult = service.receive(commandeId, receiveRequest);

        assertThat(receiveResult.commande().statut()).isEqualTo(CommandeAchatStatut.RECEPTIONNEE);
        assertThat(receiveResult.facture().numero()).isEqualTo("FACT-2026-001");
        assertThat(receiveResult.facture().statut()).isEqualTo(StatutFacture.NON_PAYEE);
        assertThat(receiveResult.facture().montantRestant()).isEqualByComparingTo("500000.00");

        verify(entreeStockDomainService).create(any());
        verify(stockDomainService).createOrUpdateEntry(any());
        verify(mouvementStockDomainService).journalize(any(), any());
    }

    // ── Scenario 2: Edit line then receive ───────────────────────────────────

    @Test
    void process_update_ligne_before_receive_applies_corrected_prices() {
        LigneCommandeAchat existing = new LigneCommandeAchat();
        existing.setId(ligneId);
        existing.setCommande(draftCommande);
        existing.setProductFournisseur(productFournisseur);
        existing.setQuantite(5);
        existing.setPrixAchat(new BigDecimal("50000.00"));
        existing.setPrixVente(new BigDecimal("70000.00"));
        draftCommande.getLignes().add(existing);

        LigneCommandeAchat corrected = new LigneCommandeAchat();
        corrected.setId(ligneId);
        corrected.setCommande(draftCommande);
        corrected.setProductFournisseur(productFournisseur);
        corrected.setQuantite(8);
        corrected.setPrixAchat(new BigDecimal("48000.00"));
        corrected.setPrixVente(new BigDecimal("72000.00"));

        when(commandeAchatService.findById(commandeId)).thenReturn(draftCommande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(draftCommande)).thenReturn(draftCommande);
        when(ligneCommandeAchatDomainService.findById(ligneId)).thenReturn(existing);
        when(ligneCommandeAchatDomainService.update(any(), any())).thenReturn(corrected);

        LigneCommandeAchatResponse updated = service.updateLigne(commandeId, ligneId,
                new LigneAchatUpdateRequest(8, new BigDecimal("48000.00"), new BigDecimal("72000.00"), "LOT-001", null));

        assertThat(updated.quantite()).isEqualTo(8);
        assertThat(updated.prixAchat()).isEqualByComparingTo("48000.00");
        assertThat(updated.prixVente()).isEqualByComparingTo("72000.00");

        verify(ligneCommandeAchatDomainService).findById(ligneId);
        verify(ligneCommandeAchatDomainService).update(eq(existing), any());
    }

    // ── Scenario 3: DRAFT → receive with initial payment ─────────────────────

    @Test
    void process_receive_with_initial_payment_updates_facture_balance() {
        LigneCommandeAchat ligne = new LigneCommandeAchat();
        ligne.setId(ligneId);
        ligne.setCommande(draftCommande);
        ligne.setProductFournisseur(productFournisseur);
        ligne.setQuantite(10);
        ligne.setPrixAchat(new BigDecimal("50000.00"));
        ligne.setPrixVente(new BigDecimal("75000.00"));
        draftCommande.getLignes().add(ligne);

        FactureAchat factureAvecPaiement = new FactureAchat();
        factureAvecPaiement.setId(UUID.randomUUID());
        factureAvecPaiement.setNumero("FACT-2026-002");
        factureAvecPaiement.setMontantTotal(new BigDecimal("500000.00"));
        factureAvecPaiement.setMontantPaye(new BigDecimal("200000.00"));
        factureAvecPaiement.setStatut(StatutFacture.PARTIELLEMENT_PAYEE);
        factureAvecPaiement.setDate(LocalDate.of(2026, 6, 3));
        factureAvecPaiement.setDateEcheance(LocalDate.of(2026, 7, 3));
        factureAvecPaiement.setCommande(draftCommande);

        CommandeAchat receptionneeCommande = new CommandeAchat();
        receptionneeCommande.setId(commandeId);
        receptionneeCommande.setStatut(CommandeAchatStatut.RECEPTIONNEE);
        receptionneeCommande.setMagasin(magasin);
        receptionneeCommande.setFournisseur(fournisseur);
        receptionneeCommande.setLignes(List.of(ligne));

        Stock stock = new Stock();
        stock.setMagasin(magasin);
        stock.setProductFournisseur(productFournisseur);
        stock.setQuantiteDisponible(10);
        stock.setPrixAchatMoyen(new BigDecimal("50000.00"));

        AchatReceiveRequest receiveRequest = new AchatReceiveRequest(
                new FactureAchatCreateRequest("FACT-2026-002",
                        LocalDate.of(2026, 6, 3), LocalDate.of(2026, 7, 3)),
                new PaiementAchatRequest(new BigDecimal("200000.00"), LocalDate.of(2026, 6, 3), MOYEN_WAVE_ID));

        when(commandeAchatService.findById(commandeId)).thenReturn(draftCommande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(draftCommande)).thenReturn(draftCommande);
        when(moyenPaiementService.findById(MOYEN_WAVE_ID)).thenReturn(moyenWave());
        when(factureAchatDomainService.create(any())).thenReturn(factureAvecPaiement);
        when(paiementAchatDomainService.create(any())).thenReturn(new org.store.achat.domain.model.PaiementAchat());
        when(factureAchatDomainService.applyPaiement(any(), any())).thenReturn(factureAvecPaiement);
        when(entreeStockDomainService.create(any())).thenReturn(new EntreeStock());
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(any(), any())).thenReturn(Optional.empty());
        when(stockDomainService.createOrUpdateEntry(any())).thenReturn(stock);
        when(commandeAchatDomainService.markReceptionnee(draftCommande)).thenReturn(receptionneeCommande);

        AchatResponse result = service.receive(commandeId, receiveRequest);

        assertThat(result.commande().statut()).isEqualTo(CommandeAchatStatut.RECEPTIONNEE);
        assertThat(result.facture().statut()).isEqualTo(StatutFacture.PARTIELLEMENT_PAYEE);
        assertThat(result.facture().montantPaye()).isEqualByComparingTo("200000.00");
        assertThat(result.facture().montantRestant()).isEqualByComparingTo("300000.00");

        verify(paiementAchatDomainService).create(any());
        verify(factureAchatDomainService).applyPaiement(any(), any());
    }

    // ── Scenario 4: RECEPTIONNEE → CANCEL with stock rollback ────────────────

    @Test
    void process_cancel_after_receive_rolls_back_stock_and_marks_annulee() {
        draftCommande.setStatut(CommandeAchatStatut.RECEPTIONNEE);

        LigneCommandeAchat ligne = new LigneCommandeAchat();
        ligne.setId(ligneId);
        ligne.setCommande(draftCommande);
        ligne.setProductFournisseur(productFournisseur);
        ligne.setQuantite(10);
        ligne.setPrixAchat(new BigDecimal("50000.00"));
        draftCommande.getLignes().add(ligne);

        EntreeStock lot = new EntreeStock();
        lot.setId(UUID.randomUUID());
        lot.setMagasin(magasin);
        lot.setProduit(productFournisseur.getProduct());
        lot.setProductFournisseur(productFournisseur);
        lot.setQuantiteInitiale(10);
        lot.setQuantiteRestante(10);

        Stock stock = new Stock();
        stock.setMagasin(magasin);
        stock.setProductFournisseur(productFournisseur);
        stock.setQuantiteDisponible(10);

        CommandeAchat annuleeCommande = new CommandeAchat();
        annuleeCommande.setId(commandeId);
        annuleeCommande.setReference("CMD-TEST-001");
        annuleeCommande.setStatut(CommandeAchatStatut.ANNULEE);
        annuleeCommande.setMotifAnnulation(MotifAnnulationAchat.ERREUR_SAISIE);
        annuleeCommande.setCommentaireAnnulation("Annulation test");
        annuleeCommande.setDateAnnulation(LocalDateTime.now());
        annuleeCommande.setMagasin(magasin);
        annuleeCommande.setFournisseur(fournisseur);

        when(commandeAchatService.findById(commandeId)).thenReturn(draftCommande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(draftCommande)).thenReturn(draftCommande);
        when(purchaseProperties.cancelWindowHours()).thenReturn(24);
        when(factureAchatDomainService.findByCommandeId(commandeId)).thenReturn(Optional.of(facture));
        when(entreeStockDomainService.findByCommandeAchatId(commandeId)).thenReturn(List.of(lot));
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(any(), any())).thenReturn(Optional.of(stock));
        when(stockDomainService.decrement(eq(stock), eq(10))).thenAnswer(inv -> {
            stock.setQuantiteDisponible(0);
            return stock;
        });
        when(commandeAchatDomainService.cancel(any(), any(), any())).thenReturn(annuleeCommande);
        when(factureAchatDomainService.cancel(facture)).thenReturn(facture);

        AnnulationAchatResponse result = service.cancel(commandeId,
                new AnnulationAchatRequest("ERREUR_SAISIE", "Annulation test"));

        assertThat(result.statut()).isEqualTo(CommandeAchatStatut.ANNULEE);
        assertThat(result.motif()).isEqualTo(MotifAnnulationAchat.ERREUR_SAISIE);

        verify(stockDomainService).decrement(eq(stock), eq(10));
        verify(mouvementStockDomainService).journalize(any(), any());
    }

    // ── Scenario 5: DRAFT → DELETE ───────────────────────────────────────────

    @Test
    void process_delete_draft_removes_commande_without_any_stock_effect() {
        when(commandeAchatService.findById(commandeId)).thenReturn(draftCommande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(draftCommande)).thenReturn(draftCommande);

        service.deleteDraft(commandeId);

        verify(commandeAchatDomainService).delete(draftCommande);
        verify(entreeStockDomainService, never()).create(any());
        verify(stockDomainService, never()).createOrUpdateEntry(any());
        verify(mouvementStockDomainService, never()).journalize(any(), any());
    }

    // ── Scenario 6: Guard — cannot cancel with consumed lots ─────────────────

    @Test
    void process_cancel_fails_when_lot_already_consumed_by_sale() {
        draftCommande.setStatut(CommandeAchatStatut.RECEPTIONNEE);

        EntreeStock partiallyConsumedLot = new EntreeStock();
        partiallyConsumedLot.setId(UUID.randomUUID());
        partiallyConsumedLot.setQuantiteInitiale(10);
        partiallyConsumedLot.setQuantiteRestante(6);

        when(commandeAchatService.findById(commandeId)).thenReturn(draftCommande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(draftCommande)).thenReturn(draftCommande);
        when(purchaseProperties.cancelWindowHours()).thenReturn(24);
        when(factureAchatDomainService.findByCommandeId(commandeId)).thenReturn(Optional.empty());
        when(entreeStockDomainService.findByCommandeAchatId(commandeId)).thenReturn(List.of(partiallyConsumedLot));

        assertThatThrownBy(() -> service.cancel(commandeId,
                new AnnulationAchatRequest("ERREUR_SAISIE", "Test")))
                .isInstanceOf(BadArgumentException.class);

        verify(stockDomainService, never()).decrement(any(Stock.class), anyInt());
    }

    // ── Scenario 7: Guard — cannot receive a non-DRAFT commande ──────────────

    @Test
    void process_receive_fails_when_commande_already_receptionnee() {
        draftCommande.setStatut(CommandeAchatStatut.RECEPTIONNEE);

        when(commandeAchatService.findById(commandeId)).thenReturn(draftCommande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(draftCommande)).thenReturn(draftCommande);

        assertThatThrownBy(() -> service.receive(commandeId,
                new AchatReceiveRequest(
                        new FactureAchatCreateRequest("F-001",
                                LocalDate.now(), LocalDate.now().plusDays(30)),
                        null)))
                .isInstanceOf(BadArgumentException.class);

        verify(factureAchatDomainService, never()).create(any());
        verify(entreeStockDomainService, never()).create(any());
    }
}
