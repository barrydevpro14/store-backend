package org.store.vente.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.achat.domain.model.Fournisseur;
import org.store.produit.domain.model.Quality;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.property.SaleProperties;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.IAccountService;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.service.ISortieStockService;
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
import org.store.vente.application.dto.LigneCommandeVenteResponse;
import org.store.vente.application.dto.LigneVenteRequest;
import org.store.vente.application.dto.LigneVenteUpdateRequest;
import org.store.vente.application.dto.PaiementVenteRequest;
import org.store.vente.application.dto.VenteDraftResponse;
import org.store.vente.application.dto.VenteRequest;
import org.store.vente.application.dto.VenteResponse;
import org.store.vente.application.dto.VenteValidateRequest;
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
import org.store.achat.domain.enums.StatutFacture;
import org.store.notification.application.service.INotificationEventPublisher;

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
 * End-to-end process tests for the sale lifecycle.
 *
 * Covers the complete sequence: DRAFT → (edit lines) → VALIDATE → CANCEL
 * and the discard path: DRAFT → DELETE
 *
 * Each test exercises multiple service calls in sequence and verifies both
 * the returned state and the side-effects (stock, facture, movements).
 */
@ExtendWith(MockitoExtension.class)
class VenteProcessFlowTest {

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
    @Mock private INotificationEventPublisher notificationEventPublisher;
    @Mock private org.store.audit.application.service.IAuditEventPublisher auditEventPublisher;

    @InjectMocks
    private VenteServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private UUID productFournisseurId;
    private UUID commandeId;
    private UUID ligneId;

    private Entreprise entreprise;
    private Magasin magasin;
    private ProductFournisseur productFournisseur;
    private Employe vendeur;
    private CommandeVente draftCommande;
    private FactureClient facture;

    @BeforeEach
    void setUp() {
        entrepriseId        = UUID.randomUUID();
        magasinId           = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();
        commandeId          = UUID.randomUUID();
        ligneId             = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setNom("Magasin Test");
        magasin.setEntreprise(entreprise);

        Product produit = new Product();
        produit.setId(UUID.randomUUID());
        produit.setNom("Pneu 205/55");
        produit.setEntreprise(entreprise);

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(UUID.randomUUID());
        fournisseur.setNom("Fournisseur Test");
        fournisseur.setEntreprise(entreprise);

        Quality quality = new Quality();
        quality.setId(UUID.randomUUID());
        quality.setLibelle("Original");

        productFournisseur = new ProductFournisseur();
        productFournisseur.setId(productFournisseurId);
        productFournisseur.setProduct(produit);
        productFournisseur.setFournisseur(fournisseur);
        productFournisseur.setQuality(quality);
        productFournisseur.setPrixAchat(new BigDecimal("8000.00"));
        productFournisseur.setPrixVente(new BigDecimal("12000.00"));

        vendeur = new Employe();
        vendeur.setId(UUID.randomUUID());
        vendeur.setNom("Diop");
        vendeur.setPrenom("Awa");
        vendeur.setMagasin(magasin);

        UUID vendeurAccountId = UUID.randomUUID();

        draftCommande = new CommandeVente();
        draftCommande.setId(commandeId);
        draftCommande.setReference("VTE-TEST-001");
        draftCommande.setStatut(CommandeVenteStatut.DRAFT);
        draftCommande.setMagasin(magasin);
        draftCommande.setDate(LocalDate.of(2026, 6, 4));
        draftCommande.setCreatedAt(LocalDateTime.now());
        draftCommande.setCreatedBy(vendeurAccountId.toString());
        draftCommande.setLignes(new ArrayList<>());

        facture = new FactureClient();
        facture.setId(UUID.randomUUID());
        facture.setNumero("FAC-VTE-001");
        facture.setMontantTotal(new BigDecimal("120000.00"));
        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setStatut(StatutFacture.NON_PAYEE);
        facture.setDate(LocalDate.of(2026, 6, 4));
        facture.setDateEcheance(LocalDate.of(2026, 7, 4));
        facture.setCommande(draftCommande);

        lenient().when(currentUserService.getCurrent()).thenReturn(new UserPrincipal(
                vendeurAccountId, null, entrepriseId, magasinId,
                "vendeur1", null, null, "SELLER", List.of()));
    }

    // ── Scenario 1: Happy path — CREATE DRAFT → VALIDATE ─────────────────────

    @Test
    void process_create_draft_then_validate_produces_validate_commande_with_facture_and_stock() {
        // ── STEP 1: Create DRAFT ─────────────────────────────────────────────
        VenteRequest createRequest = new VenteRequest(null, List.of(
                new LigneVenteRequest(productFournisseurId, 10, new BigDecimal("12000.00"))));

        when(employeService.findCurrentUser()).thenReturn(vendeur);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(commandeVenteDomainService.create(any())).thenReturn(draftCommande);
        when(ligneCommandeVenteDomainService.create(any())).thenAnswer(inv -> {
            LigneCommandeVente l = new LigneCommandeVente();
            l.setId(ligneId);
            l.setCommande(draftCommande);
            l.setProductFournisseur(productFournisseur);
            l.setQuantite(10);
            l.setPrixUnitaire(new BigDecimal("12000.00"));
            l.setMontantTotal(new BigDecimal("120000.00"));
            draftCommande.getLignes().add(l);
            return l;
        });
        when(commandeVenteDomainService.findById(commandeId)).thenReturn(draftCommande);

        VenteDraftResponse draftResult = service.create(createRequest);

        assertThat(draftResult.commande().statut()).isEqualTo(CommandeVenteStatut.DRAFT);
        assertThat(draftResult.commande().reference()).isEqualTo("VTE-TEST-001");

        // ── STEP 2: Validate → VALIDATE ──────────────────────────────────────
        CommandeVente validatedCommande = new CommandeVente();
        validatedCommande.setId(commandeId);
        validatedCommande.setReference("VTE-TEST-001");
        validatedCommande.setStatut(CommandeVenteStatut.VALIDATE);
        validatedCommande.setMagasin(magasin);
        validatedCommande.setDate(LocalDate.of(2026, 6, 4));
        validatedCommande.setLignes(draftCommande.getLignes());

        VenteValidateRequest validateRequest = new VenteValidateRequest(
                LocalDate.of(2026, 7, 4), null);

        when(commandeVenteDomainService.validate(draftCommande)).thenReturn(validatedCommande);
        when(factureClientDomainService.create(any())).thenReturn(facture);
        when(accountService.findUserSummaryByAccountId(any())).thenReturn(Optional.empty());

        VenteResponse validateResult = service.validate(commandeId, validateRequest);

        assertThat(validateResult.commande().statut()).isEqualTo(CommandeVenteStatut.VALIDATE);
        assertThat(validateResult.commande().reference()).isEqualTo("VTE-TEST-001");
        assertThat(validateResult.facture().numero()).isEqualTo("FAC-VTE-001");
        assertThat(validateResult.facture().statut()).isEqualTo(StatutFacture.NON_PAYEE);
        assertThat(validateResult.facture().montantTotal()).isEqualByComparingTo("120000.00");

        // Stock consumed per ligne
        verify(sortieStockService).consumeForVente(any());
        verify(factureClientDomainService).create(any());
        verify(commandeVenteDomainService).validate(draftCommande);
    }

    // ── Scenario 2: Edit ligne before validate ───────────────────────────────

    @Test
    void process_update_ligne_before_validate_applies_corrected_price() {
        LigneCommandeVente existing = new LigneCommandeVente();
        existing.setId(ligneId);
        existing.setCommande(draftCommande);
        existing.setProductFournisseur(productFournisseur);
        existing.setQuantite(5);
        existing.setPrixUnitaire(new BigDecimal("12000.00"));
        existing.setMontantTotal(new BigDecimal("60000.00"));
        draftCommande.getLignes().add(existing);

        LigneCommandeVente corrected = new LigneCommandeVente();
        corrected.setId(ligneId);
        corrected.setCommande(draftCommande);
        corrected.setProductFournisseur(productFournisseur);
        corrected.setQuantite(8);
        corrected.setPrixUnitaire(new BigDecimal("13000.00"));
        corrected.setMontantTotal(new BigDecimal("104000.00"));

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(draftCommande);
        when(ligneCommandeVenteDomainService.findById(ligneId)).thenReturn(existing);
        when(ligneCommandeVenteDomainService.update(eq(existing), eq(8), eq(new BigDecimal("13000.00")))).thenReturn(corrected);

        LigneCommandeVenteResponse result = service.updateLigne(commandeId, ligneId,
                new LigneVenteUpdateRequest(8, new BigDecimal("13000.00")));

        assertThat(result.quantite()).isEqualTo(8);
        assertThat(result.prixUnitaire()).isEqualByComparingTo("13000.00");

        verify(ligneCommandeVenteDomainService).update(eq(existing), eq(8), eq(new BigDecimal("13000.00")));
    }

    // ── Scenario 3: DRAFT → VALIDATE with initial payment ────────────────────

    @Test
    void process_validate_with_initial_payment_sets_facture_partiellement_payee() {
        LigneCommandeVente ligne = new LigneCommandeVente();
        ligne.setId(ligneId);
        ligne.setCommande(draftCommande);
        ligne.setProductFournisseur(productFournisseur);
        ligne.setQuantite(10);
        ligne.setPrixUnitaire(new BigDecimal("12000.00"));
        ligne.setMontantTotal(new BigDecimal("120000.00"));
        draftCommande.getLignes().add(ligne);

        FactureClient factureAvecPaiement = new FactureClient();
        factureAvecPaiement.setId(UUID.randomUUID());
        factureAvecPaiement.setNumero("FAC-VTE-002");
        factureAvecPaiement.setMontantTotal(new BigDecimal("120000.00"));
        factureAvecPaiement.setMontantPaye(new BigDecimal("50000.00"));
        factureAvecPaiement.setStatut(StatutFacture.PARTIELLEMENT_PAYEE);
        factureAvecPaiement.setDate(LocalDate.of(2026, 6, 4));
        factureAvecPaiement.setDateEcheance(LocalDate.of(2026, 7, 4));
        factureAvecPaiement.setCommande(draftCommande);

        CommandeVente validatedCommande = new CommandeVente();
        validatedCommande.setId(commandeId);
        validatedCommande.setStatut(CommandeVenteStatut.VALIDATE);
        validatedCommande.setMagasin(magasin);
        validatedCommande.setDate(LocalDate.of(2026, 6, 4));
        validatedCommande.setLignes(List.of(ligne));

        VenteValidateRequest validateRequest = new VenteValidateRequest(
                LocalDate.of(2026, 7, 4),
                new PaiementVenteRequest(new BigDecimal("50000.00"), "CASH", null));

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(draftCommande);
        when(factureClientDomainService.create(any())).thenReturn(facture);
        when(paiementVenteDomainService.create(any())).thenReturn(new org.store.vente.domain.model.PaiementVente());
        when(factureClientDomainService.applyPaiement(facture, new BigDecimal("50000.00"))).thenReturn(factureAvecPaiement);
        when(commandeVenteDomainService.validate(draftCommande)).thenReturn(validatedCommande);
        when(accountService.findUserSummaryByAccountId(any())).thenReturn(Optional.empty());

        VenteResponse result = service.validate(commandeId, validateRequest);

        assertThat(result.facture().statut()).isEqualTo(StatutFacture.PARTIELLEMENT_PAYEE);
        assertThat(result.facture().montantPaye()).isEqualByComparingTo("50000.00");
        assertThat(result.facture().montantRestant()).isEqualByComparingTo("70000.00");

        verify(paiementVenteDomainService).create(any());
        verify(factureClientDomainService).applyPaiement(eq(facture), eq(new BigDecimal("50000.00")));
    }

    // ── Scenario 4: VALIDATE → CANCEL with stock re-injection ────────────────

    @Test
    void process_cancel_after_validate_reinjects_stock_and_marks_cancel() {
        draftCommande.setStatut(CommandeVenteStatut.VALIDATE);

        LigneCommandeVente ligne = new LigneCommandeVente();
        ligne.setId(ligneId);
        ligne.setCommande(draftCommande);
        ligne.setProductFournisseur(productFournisseur);
        ligne.setQuantite(10);
        ligne.setPrixUnitaire(new BigDecimal("12000.00"));
        ligne.setMontantTotal(new BigDecimal("120000.00"));
        draftCommande.getLignes().add(ligne);

        EntreeStock lot = new EntreeStock();
        lot.setId(UUID.randomUUID());
        lot.setMagasin(magasin);
        lot.setProduit(productFournisseur.getProduct());
        lot.setProductFournisseur(productFournisseur);
        lot.setQuantiteRestante(10);

        SortieStock sortie = new SortieStock();
        sortie.setId(UUID.randomUUID());
        sortie.setEntreeStock(lot);
        sortie.setQuantiteSortie(10);
        sortie.setAnnulee(false);

        Stock stock = new Stock();
        stock.setMagasin(magasin);
        stock.setProductFournisseur(productFournisseur);
        stock.setQuantiteDisponible(10);

        Stock stockApres = new Stock();
        stockApres.setMagasin(magasin);
        stockApres.setProductFournisseur(productFournisseur);
        stockApres.setQuantiteDisponible(20);

        CommandeVente cancelledCommande = new CommandeVente();
        cancelledCommande.setId(commandeId);
        cancelledCommande.setReference("VTE-TEST-001");
        cancelledCommande.setStatut(CommandeVenteStatut.CANCEL);
        cancelledCommande.setMagasin(magasin);
        cancelledCommande.setMotifAnnulation(MotifAnnulationVente.ERREUR_SAISIE);
        cancelledCommande.setCommentaireAnnulation("Annulation test");
        cancelledCommande.setDateAnnulation(LocalDateTime.now());

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(draftCommande);
        when(saleProperties.cancelWindowHours()).thenReturn(24);
        when(sortieStockDomainService.findActiveByLigneVenteId(ligneId)).thenReturn(List.of(sortie));
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, productFournisseurId))
                .thenReturn(Optional.of(stock));
        when(stockDomainService.creditQuantite(stock, 10)).thenReturn(stockApres);
        when(commandeVenteDomainService.cancel(any(), eq(MotifAnnulationVente.ERREUR_SAISIE), any()))
                .thenReturn(cancelledCommande);
        when(factureClientDomainService.findByCommandeId(commandeId)).thenReturn(Optional.of(facture));
        when(factureClientDomainService.cancel(facture)).thenReturn(facture);

        AnnulationVenteResponse result = service.cancel(commandeId,
                new AnnulationVenteRequest("ERREUR_SAISIE", "Annulation test"));

        assertThat(result.statut()).isEqualTo(CommandeVenteStatut.CANCEL);
        assertThat(result.motif()).isEqualTo(MotifAnnulationVente.ERREUR_SAISIE);
        assertThat(result.totalQuantiteReinjectee()).isEqualTo(10);
        assertThat(result.nombreMouvementsCrees()).isEqualTo(1);

        // Stock was re-injected
        verify(entreeStockDomainService).creditQuantiteRestante(lot, 10);
        verify(sortieStockDomainService).markAsAnnulee(sortie);
        verify(stockDomainService).creditQuantite(stock, 10);
        verify(mouvementStockDomainService).journalize(eq(stockApres), any());
        // Facture cancelled
        verify(factureClientDomainService).cancel(facture);
    }

    // ── Scenario 5: DRAFT → DELETE ───────────────────────────────────────────

    @Test
    void process_delete_draft_removes_commande_without_any_stock_effect() {
        when(commandeVenteDomainService.findById(commandeId)).thenReturn(draftCommande);

        service.deleteDraft(commandeId);

        verify(commandeVenteDomainService).delete(draftCommande);
        verify(sortieStockService, never()).consumeForVente(any());
        verify(factureClientDomainService, never()).create(any());
    }

    // ── Scenario 6: Guard — cannot cancel a DRAFT commande ───────────────────

    @Test
    void process_cancel_fails_when_commande_is_draft() {
        when(commandeVenteDomainService.findById(commandeId)).thenReturn(draftCommande);

        assertThatThrownBy(() -> service.cancel(commandeId,
                new AnnulationVenteRequest("ERREUR_SAISIE", "Test")))
                .isInstanceOf(BadArgumentException.class);

        verify(sortieStockDomainService, never()).findActiveByLigneVenteId(any());
        verify(stockDomainService, never()).creditQuantite(any(Stock.class), anyInt());
    }

    // ── Scenario 7: Guard — cannot validate a non-DRAFT commande ─────────────

    @Test
    void process_validate_fails_when_commande_already_validated() {
        draftCommande.setStatut(CommandeVenteStatut.VALIDATE);

        when(commandeVenteDomainService.findById(commandeId)).thenReturn(draftCommande);

        assertThatThrownBy(() -> service.validate(commandeId,
                new VenteValidateRequest(LocalDate.now().plusDays(30), null)))
                .isInstanceOf(BadArgumentException.class);

        verify(sortieStockService, never()).consumeForVente(any());
        verify(factureClientDomainService, never()).create(any());
    }
}
