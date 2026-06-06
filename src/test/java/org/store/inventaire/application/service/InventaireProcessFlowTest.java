package org.store.inventaire.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.ValidatorService;
import org.store.depense.domain.service.DepenseDomainService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.inventaire.application.dto.BilanInventaireRequest;
import org.store.inventaire.application.dto.InventaireResponse;
import org.store.inventaire.application.dto.LigneInventaireRequest;
import org.store.inventaire.application.dto.LigneInventaireResponse;
import org.store.inventaire.application.dto.LigneInventaireUpdateRequest;
import org.store.inventaire.application.service.impl.InventaireServiceImpl;
import org.store.inventaire.domain.enums.InventaireStatut;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.model.LigneInventaire;
import org.store.inventaire.domain.service.InventaireDomainService;
import org.store.inventaire.domain.service.LigneInventaireDomainService;
import org.store.inventaire.domain.service.RapportInventaireDomainService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.produit.domain.model.Quality;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.service.IAjustementStockService;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.service.EntreeStockDomainService;

import java.math.BigDecimal;
import java.time.LocalDate;
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

/**
 * End-to-end process tests for the inventory lifecycle.
 *
 * Covers the complete sequence:
 *   EN_COURS → (add/edit/delete lignes) → BILAN → CLOTURE (adjustments applied)
 * and the abandon path:
 *   EN_COURS|BILAN → ANNULE (no stock effect)
 *
 * Each test exercises multiple service calls in sequence and verifies
 * both the returned state and the side-effects (stock adjustments, rapport).
 */
@ExtendWith(MockitoExtension.class)
class InventaireProcessFlowTest {

    @Mock private InventaireDomainService inventaireDomainService;
    @Mock private LigneInventaireDomainService ligneInventaireDomainService;
    @Mock private RapportInventaireDomainService rapportInventaireDomainService;
    @Mock private EntreeStockDomainService entreeStockDomainService;
    @Mock private DepenseDomainService depenseDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private IProductFournisseurService productFournisseurService;
    @Mock private IAjustementStockService ajustementStockService;
    @Mock private org.store.stock.domain.service.StockDomainService stockDomainService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;
    @Mock private IMessageSourceService messageSourceService;

    @InjectMocks
    private InventaireServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private UUID inventaireId;
    private UUID ligneId;
    private UUID productFournisseurId;

    private Entreprise entreprise;
    private Magasin magasin;
    private ProductFournisseur productFournisseur;
    private Inventaire inventaire;

    @BeforeEach
    void setUp() {
        entrepriseId       = UUID.randomUUID();
        magasinId          = UUID.randomUUID();
        inventaireId       = UUID.randomUUID();
        ligneId            = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setNom("Magasin Test");
        magasin.setEntreprise(entreprise);

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(UUID.randomUUID());
        fournisseur.setNom("Fournisseur Test");
        fournisseur.setEntreprise(entreprise);

        Product produit = new Product();
        produit.setId(UUID.randomUUID());
        produit.setNom("Pneu 205/55");
        produit.setReference("PN-205");
        produit.setEntreprise(entreprise);

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

        inventaire = new Inventaire();
        inventaire.setId(inventaireId);
        inventaire.setMagasin(magasin);
        inventaire.setStatut(InventaireStatut.EN_COURS);
        inventaire.setDate(LocalDate.of(2026, 6, 4));
        inventaire.setLignes(new ArrayList<>());

        lenient().when(currentUserService.getCurrent()).thenReturn(new UserPrincipal(
                UUID.randomUUID(), null, entrepriseId, magasinId,
                "manager", null, null, "MANAGER", List.of()));
    }

    private LigneInventaire buildLigne(int qteTheorique, int qteReelle) {
        LigneInventaire ligne = new LigneInventaire();
        ligne.setId(ligneId);
        ligne.setInventaire(inventaire);
        ligne.setProductFournisseur(productFournisseur);
        ligne.setQuantiteTheorique(qteTheorique);
        ligne.setQuantiteReelle(qteReelle);
        ligne.setEcart(qteReelle - qteTheorique);
        ligne.setPrixUnitaire(productFournisseur.getPrixAchat());
        return ligne;
    }

    // ── Scenario 1: Full lifecycle EN_COURS → ADD LIGNE → BILAN → CLOTURE ───

    @Test
    void process_create_add_ligne_bilan_then_cloturer_applies_stock_adjustment() {
        // ── STEP 1: Create EN_COURS ──────────────────────────────────────────
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(inventaireDomainService.hasActiveInventaire(magasinId)).thenReturn(false);
        when(inventaireDomainService.create(eq(magasin), any())).thenReturn(inventaire);

        InventaireResponse createResult = service.create(magasinId);

        assertThat(createResult.statut()).isEqualTo(InventaireStatut.EN_COURS);
        assertThat(createResult.magasin().id()).isEqualTo(magasinId);

        // ── STEP 2: Add ligne (5 lots theoriques, 4 counted physically) ─────
        EntreeStock lot = new EntreeStock();
        lot.setQuantiteRestante(5);

        LigneInventaire ligne = buildLigne(5, 4); // ecart = -1

        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaire);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(ligneInventaireDomainService.findByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId)).thenReturn(Optional.empty());
        when(entreeStockDomainService.findAvailableLotsForFifoByProductFournisseur(magasinId, productFournisseurId)).thenReturn(List.of(lot));
        when(ligneInventaireDomainService.create(eq(inventaire), eq(productFournisseur), eq(5), eq(4), any(java.math.BigDecimal.class))).thenReturn(ligne);

        LigneInventaireResponse ligneResult = service.addLigne(inventaireId,
                new LigneInventaireRequest(productFournisseurId, 4, new java.math.BigDecimal("8000.00")));

        assertThat(ligneResult.quantiteTheorique()).isEqualTo(5);
        assertThat(ligneResult.quantiteReelle()).isEqualTo(4);
        assertThat(ligneResult.ecart()).isEqualTo(-1);

        // ── STEP 3: Passer EN BILAN (fige les saisies + rapport comptable) ───
        Inventaire bilanInventaire = new Inventaire();
        bilanInventaire.setId(inventaireId);
        bilanInventaire.setMagasin(magasin);
        bilanInventaire.setStatut(InventaireStatut.BILAN);
        bilanInventaire.setDate(LocalDate.of(2026, 6, 4));

        when(ligneInventaireDomainService.findAllByInventaireId(inventaireId)).thenReturn(List.of(ligne));
        when(inventaireDomainService.transitionStatut(inventaire, InventaireStatut.BILAN)).thenReturn(bilanInventaire);
        when(depenseDomainService.computeTotal(any(), eq(entrepriseId))).thenReturn(null);

        InventaireResponse bilanResult = service.passerEnBilan(inventaireId, new BilanInventaireRequest(
                new BigDecimal("500000.00"), new BigDecimal("200000.00"),
                LocalDate.of(2026, 6, 1)));

        assertThat(bilanResult.statut()).isEqualTo(InventaireStatut.BILAN);
        verify(rapportInventaireDomainService).create(eq(bilanInventaire), any());

        // ── STEP 4: Cloturer → ajustement applique pour ecart != 0 ──────────
        Inventaire clotureInventaire = new Inventaire();
        clotureInventaire.setId(inventaireId);
        clotureInventaire.setMagasin(magasin);
        clotureInventaire.setStatut(InventaireStatut.CLOTURE);
        clotureInventaire.setDate(LocalDate.of(2026, 6, 4));

        org.store.stock.domain.model.Stock stock = new org.store.stock.domain.model.Stock();
        stock.setMagasin(magasin);
        stock.setProductFournisseur(productFournisseur);
        stock.setQuantiteDisponible(5);

        when(inventaireDomainService.findById(inventaireId)).thenReturn(bilanInventaire);
        when(ligneInventaireDomainService.findAllByInventaireId(inventaireId)).thenReturn(List.of(ligne));
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, productFournisseurId))
                .thenReturn(Optional.of(stock));
        when(messageSourceService.getMessage(any(String.class), any(Object[].class))).thenReturn("Clôture inventaire");
        when(inventaireDomainService.transitionStatut(bilanInventaire, InventaireStatut.CLOTURE)).thenReturn(clotureInventaire);

        InventaireResponse clotureResult = service.cloturer(inventaireId, null);

        assertThat(clotureResult.statut()).isEqualTo(InventaireStatut.CLOTURE);
        // Adjustment must have been triggered for the line with ecart = -1
        verify(ajustementStockService).create(any());
    }

    // ── Scenario 1b: Adding same product again aggregates the count ──────────

    @Test
    void process_add_ligne_twice_aggregates_quantite_reelle() {
        LigneInventaire existing = buildLigne(5, 4);

        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaire);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(ligneInventaireDomainService.findByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId))
                .thenReturn(Optional.of(existing));

        LigneInventaire aggregated = buildLigne(5, 7); // 4 + 3 = 7
        when(ligneInventaireDomainService.updateQuantiteReelle(existing, 7)).thenReturn(aggregated);

        LigneInventaireResponse result = service.addLigne(inventaireId,
                new LigneInventaireRequest(productFournisseurId, 3, new java.math.BigDecimal("8000.00")));

        assertThat(result.quantiteReelle()).isEqualTo(7);
        assertThat(result.ecart()).isEqualTo(2); // 7 - 5 = +2 surplus
    }

    // ── Scenario 2: Update ligne corrects the physical count ─────────────────

    @Test
    void process_update_ligne_corrects_quantite_reelle() {
        LigneInventaire existing = buildLigne(5, 4);
        LigneInventaire corrected = buildLigne(5, 6); // correction: found more

        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaire);
        when(ligneInventaireDomainService.findLigne(ligneId)).thenReturn(existing);
        when(ligneInventaireDomainService.updateQuantiteReelle(existing, 6)).thenReturn(corrected);

        LigneInventaireResponse result = service.updateLigne(inventaireId, ligneId,
                new LigneInventaireUpdateRequest(6));

        assertThat(result.quantiteReelle()).isEqualTo(6);
        assertThat(result.ecart()).isEqualTo(1); // 6 - 5 = +1 surplus

        verify(ligneInventaireDomainService).updateQuantiteReelle(existing, 6);
    }

    // ── Scenario 3: Delete ligne removes it from the inventory ───────────────

    @Test
    void process_delete_ligne_removes_line_from_en_cours_inventory() {
        LigneInventaire ligne = buildLigne(5, 4);

        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaire);
        when(ligneInventaireDomainService.findLigne(ligneId)).thenReturn(ligne);

        service.deleteLigne(inventaireId, ligneId);

        verify(ligneInventaireDomainService).delete(ligne);
        // No stock side-effect when just removing a ligne
        verify(ajustementStockService, never()).create(any());
    }

    // ── Scenario 4: CREATE → ANNULER from EN_COURS (no stock adjustment) ─────

    @Test
    void process_annuler_from_en_cours_abandons_without_stock_effect() {
        Inventaire annuleInventaire = new Inventaire();
        annuleInventaire.setId(inventaireId);
        annuleInventaire.setMagasin(magasin);
        annuleInventaire.setStatut(InventaireStatut.ANNULE);
        annuleInventaire.setDate(LocalDate.of(2026, 6, 4));

        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaire);
        when(inventaireDomainService.transitionStatut(inventaire, InventaireStatut.ANNULE)).thenReturn(annuleInventaire);

        InventaireResponse result = service.annuler(inventaireId);

        assertThat(result.statut()).isEqualTo(InventaireStatut.ANNULE);
        // No stock adjustments applied
        verify(ajustementStockService, never()).create(any());
        verify(rapportInventaireDomainService, never()).create(any(), any());
    }

    // ── Scenario 5: BILAN → ANNULER (discrepancies discarded) ────────────────

    @Test
    void process_annuler_from_bilan_discards_ecarts_without_adjustments() {
        inventaire.setStatut(InventaireStatut.BILAN);

        Inventaire annuleInventaire = new Inventaire();
        annuleInventaire.setId(inventaireId);
        annuleInventaire.setMagasin(magasin);
        annuleInventaire.setStatut(InventaireStatut.ANNULE);
        annuleInventaire.setDate(LocalDate.of(2026, 6, 4));

        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaire);
        when(inventaireDomainService.transitionStatut(inventaire, InventaireStatut.ANNULE)).thenReturn(annuleInventaire);

        InventaireResponse result = service.annuler(inventaireId);

        assertThat(result.statut()).isEqualTo(InventaireStatut.ANNULE);
        verify(ajustementStockService, never()).create(any());
    }

    // ── Scenario 6: Guard — cannot create if an active inventory already exists

    @Test
    void process_create_fails_when_active_inventory_already_open() {
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(inventaireDomainService.hasActiveInventaire(magasinId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(magasinId))
                .isInstanceOf(BadArgumentException.class);

        verify(inventaireDomainService, never()).create(any(), any());
    }

    // ── Scenario 7: Guard — cannot add a ligne once statut moved to BILAN ────

    @Test
    void process_add_ligne_fails_when_inventory_is_in_bilan_statut() {
        inventaire.setStatut(InventaireStatut.BILAN);

        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaire);

        assertThatThrownBy(() -> service.addLigne(inventaireId,
                new LigneInventaireRequest(productFournisseurId, 3, new java.math.BigDecimal("8000.00"))))
                .isInstanceOf(BadArgumentException.class);

        verify(ligneInventaireDomainService, never()).create(any(), any(), any(int.class), any(int.class), any());
    }
}
