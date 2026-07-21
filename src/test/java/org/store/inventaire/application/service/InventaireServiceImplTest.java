package org.store.inventaire.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.ValidatorService;
import org.store.depense.application.dto.DepenseFilter;
import org.store.depense.application.dto.DepenseTotalResponse;
import org.store.depense.application.service.IDepenseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.inventaire.application.dto.InventaireFilter;
import org.store.inventaire.application.dto.InventaireResponse;
import org.store.inventaire.application.dto.LigneInventaireRequest;
import org.store.inventaire.application.dto.BilanInventaireRequest;
import org.store.inventaire.application.dto.LigneInventaireResponse;
import org.store.inventaire.application.dto.LigneInventaireUpdateRequest;
import org.store.inventaire.application.dto.RapportInventaireCommand;
import org.store.inventaire.application.service.ILigneInventaireService;
import org.store.inventaire.application.service.IRapportInventaireService;
import org.store.inventaire.application.service.impl.InventaireServiceImpl;
import org.store.inventaire.domain.enums.InventaireStatut;
import org.store.inventaire.domain.enums.TypeInventaire;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.model.LigneInventaire;
import org.store.inventaire.domain.service.InventaireDomainService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.achat.domain.model.Fournisseur;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.AjustementStockRequest;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.application.service.IAjustementStockService;
import org.store.stock.application.service.IEntreeStockService;
import org.store.stock.application.service.IStockService;
import org.store.stock.domain.enums.MotifAjustement;
import org.store.stock.domain.enums.TypeAjustement;
import org.store.stock.domain.model.EntreeStock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventaireServiceImplTest {

    @Mock private InventaireDomainService inventaireDomainService;
    @Mock private ILigneInventaireService ligneInventaireService;
    @Mock private IRapportInventaireService rapportInventaireService;
    @Mock private IEntreeStockService entreeStockService;
    @Mock private IStockService stockService;
    @Mock private IDepenseService depenseService;
    @Mock private IMagasinService magasinService;
    @Mock private IProductFournisseurService productFournisseurService;
    @Mock private IAjustementStockService ajustementStockService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;
    @Mock private IMessageSourceService messageSourceService;

    @InjectMocks
    private InventaireServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private UUID inventaireId;
    private UUID productFournisseurId;
    private UUID productId;
    private Entreprise entreprise;
    private Magasin magasin;
    private Product product;
    private ProductFournisseur productFournisseur;
    private Inventaire inventaireEnCours;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
        inventaireId = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();
        productId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);

        product = new Product();
        product.setId(productId);
        product.setEntreprise(entreprise);

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(UUID.randomUUID());
        fournisseur.setNom("Fournisseur Test");

        productFournisseur = new ProductFournisseur();
        productFournisseur.setId(productFournisseurId);
        productFournisseur.setProduct(product);
        productFournisseur.setFournisseur(fournisseur);
        productFournisseur.setPrixAchat(new BigDecimal("10.00"));

        inventaireEnCours = new Inventaire();
        inventaireEnCours.setId(inventaireId);
        inventaireEnCours.setMagasin(magasin);
        inventaireEnCours.setType(TypeInventaire.PHYSIQUE);
        inventaireEnCours.setStatut(InventaireStatut.EN_COURS);
        inventaireEnCours.setDate(LocalDate.now());
    }

    private UserPrincipal currentUser() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, magasinId,
                "owner", null, null, "OWNER", List.of("STOCK_INVENTORY"));
    }

    private EntreeStock lot(int quantiteRestante) {
        EntreeStock lot = new EntreeStock();
        lot.setId(UUID.randomUUID());
        lot.setQuantiteRestante(quantiteRestante);
        return lot;
    }

    private LigneInventaire ligne(int qteTheorique, int qteReelle) {
        return ligne(qteTheorique, qteReelle, productFournisseur.getPrixAchat());
    }

    private LigneInventaire ligne(int qteTheorique, int qteReelle, BigDecimal prixUnitaire) {
        LigneInventaire ligne = new LigneInventaire();
        ligne.setId(UUID.randomUUID());
        ligne.setInventaire(inventaireEnCours);
        ligne.setProductFournisseur(productFournisseur);
        ligne.setQuantiteTheorique(qteTheorique);
        ligne.setQuantiteReelle(qteReelle);
        ligne.setEcart(qteReelle - qteTheorique);
        ligne.setPrixUnitaire(prixUnitaire);
        return ligne;
    }

    private BilanInventaireRequest bilanRequest(String caisse, String roulement, LocalDate dateDebut) {
        return new BilanInventaireRequest(new BigDecimal(caisse), new BigDecimal(roulement), dateDebut);
    }

    @Test
    void create_should_create_inventaire_en_cours_for_current_date() {
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(inventaireDomainService.hasActiveInventaire(magasinId)).thenReturn(false);
        when(inventaireDomainService.create(eq(magasin), any(LocalDate.class), any(TypeInventaire.class))).thenReturn(inventaireEnCours);

        InventaireResponse response = service.create(magasinId, TypeInventaire.PHYSIQUE);

        assertThat(response.id()).isEqualTo(inventaireId);
        assertThat(response.statut()).isEqualTo(InventaireStatut.EN_COURS);
        verify(inventaireDomainService).create(eq(magasin), any(LocalDate.class), eq(TypeInventaire.PHYSIQUE));
    }

    @Test
    void create_should_throw_when_active_inventaire_already_exists_en_cours() {
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(inventaireDomainService.hasActiveInventaire(magasinId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(magasinId, TypeInventaire.PHYSIQUE))
                .isInstanceOf(BadArgumentException.class);

        verify(inventaireDomainService, never()).create(any(), any(), any());
    }

    @Test
    void create_should_throw_when_active_inventaire_already_exists_bilan() {
        inventaireEnCours.setStatut(InventaireStatut.BILAN);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(inventaireDomainService.hasActiveInventaire(magasinId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(magasinId, TypeInventaire.PHYSIQUE))
                .isInstanceOf(BadArgumentException.class);

        verify(inventaireDomainService, never()).create(any(), any(), any());
    }

    @Test
    void addLigne_should_compute_quantiteTheorique_from_lots_and_persist_ligne() {
        BigDecimal prix = new BigDecimal("10.00");
        LigneInventaireRequest request = new LigneInventaireRequest(productFournisseurId, 8, prix);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(ligneInventaireService.findByInventaireIdAndProductFournisseurId(inventaireId, productFournisseurId))
                .thenReturn(Optional.empty());
        when(entreeStockService.findAvailableLotsForFifo(magasinId, productFournisseurId))
                .thenReturn(List.of(lot(5), lot(5)));
        when(ligneInventaireService.create(eq(inventaireEnCours), eq(productFournisseur), eq(10), eq(8), eq(prix)))
                .thenReturn(ligne(10, 8));

        LigneInventaireResponse response = service.addLigne(inventaireId, request);

        assertThat(response.quantiteTheorique()).isEqualTo(10);
        assertThat(response.quantiteReelle()).isEqualTo(8);
        assertThat(response.ecart()).isEqualTo(-2);
        verify(ligneInventaireService).create(eq(inventaireEnCours), eq(productFournisseur), eq(10), eq(8), eq(prix));
    }

    @Test
    void addLigne_should_throw_when_inventaire_not_en_cours() {
        inventaireEnCours.setStatut(InventaireStatut.BILAN);
        LigneInventaireRequest request = new LigneInventaireRequest(productFournisseurId, 3, new BigDecimal("10.00"));

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);

        assertThatThrownBy(() -> service.addLigne(inventaireId, request))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void addLigne_should_throw_when_inventaire_other_entreprise() {
        Entreprise autre = new Entreprise();
        autre.setId(UUID.randomUUID());
        magasin.setEntreprise(autre);
        LigneInventaireRequest request = new LigneInventaireRequest(productFournisseurId, 3, new BigDecimal("10.00"));

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);

        assertThatThrownBy(() -> service.addLigne(inventaireId, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateLigne_should_update_quantite_and_recompute_ecart() {
        UUID ligneId = UUID.randomUUID();
        LigneInventaire existing = ligne(10, 8);
        existing.setId(ligneId);
        LigneInventaire updated = ligne(10, 7);
        updated.setId(ligneId);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);
        when(ligneInventaireService.findLigne(ligneId)).thenReturn(existing);
        when(ligneInventaireService.updateQuantiteReelle(existing, 7)).thenReturn(updated);

        LigneInventaireResponse response = service.updateLigne(inventaireId, ligneId, new LigneInventaireUpdateRequest(7));

        assertThat(response.quantiteReelle()).isEqualTo(7);
        assertThat(response.ecart()).isEqualTo(-3);
        verify(ligneInventaireService).updateQuantiteReelle(existing, 7);
    }

    @Test
    void updateLigne_should_throw_when_inventaire_not_en_cours() {
        inventaireEnCours.setStatut(InventaireStatut.BILAN);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);

        UUID randomLigneId = UUID.randomUUID();
        LigneInventaireUpdateRequest updateReq = new LigneInventaireUpdateRequest(5);

        assertThatThrownBy(() -> service.updateLigne(inventaireId, randomLigneId, updateReq))
                .isInstanceOf(BadArgumentException.class);

        verify(ligneInventaireService, never()).updateQuantiteReelle(any(), eq(5));
    }

    @Test
    void updateLigne_should_throw_when_ligne_belongs_to_other_inventaire() {
        UUID ligneId = UUID.randomUUID();
        Inventaire autreInventaire = new Inventaire();
        autreInventaire.setId(UUID.randomUUID());
        autreInventaire.setMagasin(magasin);
        LigneInventaire ligneAutre = new LigneInventaire();
        ligneAutre.setId(ligneId);
        ligneAutre.setInventaire(autreInventaire);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);
        when(ligneInventaireService.findLigne(ligneId)).thenReturn(ligneAutre);

        LigneInventaireUpdateRequest updateReq = new LigneInventaireUpdateRequest(5);

        assertThatThrownBy(() -> service.updateLigne(inventaireId, ligneId, updateReq))
                .isInstanceOf(BadArgumentException.class);

        verify(ligneInventaireService, never()).updateQuantiteReelle(any(), eq(5));
    }

    @Test
    void deleteLigne_should_delete_when_en_cours_and_ligne_matches() {
        UUID ligneId = UUID.randomUUID();
        LigneInventaire existing = ligne(10, 8);
        existing.setId(ligneId);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);
        when(ligneInventaireService.findLigne(ligneId)).thenReturn(existing);

        service.deleteLigne(inventaireId, ligneId);

        verify(ligneInventaireService).delete(existing);
    }

    @Test
    void deleteLigne_should_throw_when_inventaire_not_en_cours() {
        inventaireEnCours.setStatut(InventaireStatut.BILAN);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);

        UUID randomLigneId = UUID.randomUUID();

        assertThatThrownBy(() -> service.deleteLigne(inventaireId, randomLigneId))
                .isInstanceOf(BadArgumentException.class);

        verify(ligneInventaireService, never()).delete(any());
    }

    @Test
    void passerEnBilan_should_transition_and_delegate_rapport_creation_with_computed_command() {
        LigneInventaire ligne = ligne(10, 12, new BigDecimal("15.00"));
        Inventaire bilan = new Inventaire();
        bilan.setId(inventaireId);
        bilan.setMagasin(magasin);
        bilan.setStatut(InventaireStatut.BILAN);
        bilan.setDate(LocalDate.now());
        LocalDate dateDebut = LocalDate.now().minusDays(7);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);
        when(ligneInventaireService.findAllByInventaireId(inventaireId)).thenReturn(List.of(ligne));
        when(inventaireDomainService.transitionStatut(inventaireEnCours, InventaireStatut.BILAN)).thenReturn(bilan);
        when(depenseService.computeTotal(any(DepenseFilter.class)))
                .thenReturn(new DepenseTotalResponse(magasinId, new BigDecimal("50.00"), 1L));

        InventaireResponse response = service.passerEnBilan(inventaireId, bilanRequest("500.00", "400.00", dateDebut));

        assertThat(response.statut()).isEqualTo(InventaireStatut.BILAN);
        ArgumentCaptor<RapportInventaireCommand> captor = ArgumentCaptor.forClass(RapportInventaireCommand.class);
        verify(rapportInventaireService).create(eq(bilan), captor.capture());
        RapportInventaireCommand command = captor.getValue();
        // 10 × 15.00 = 150.00 (théorique × prixUnitaire saisi)
        assertThat(command.montantAutomatique()).isEqualByComparingTo("150.00");
        // 12 × 15.00 = 180.00 (réelle × prixUnitaire saisi)
        assertThat(command.montantPhysique()).isEqualByComparingTo("180.00");
        assertThat(command.montantCaisse()).isEqualByComparingTo("500.00");
        assertThat(command.depense()).isEqualByComparingTo("50.00");
        assertThat(command.montantRoulement()).isEqualByComparingTo("400.00");
        assertThat(command.dateDebutPeriode()).isEqualTo(dateDebut);
        assertThat(command.dateFinPeriode()).isEqualTo(LocalDate.now());
        verify(ajustementStockService, never()).create(any());
    }

    @Test
    void passerEnBilan_should_throw_when_inventaire_not_en_cours() {
        inventaireEnCours.setStatut(InventaireStatut.BILAN);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);

        BilanInventaireRequest bilan = bilanRequest("0.00", "0.00", LocalDate.now());

        assertThatThrownBy(() -> service.passerEnBilan(inventaireId, bilan))
                .isInstanceOf(BadArgumentException.class);

        verify(rapportInventaireService, never()).create(any(), any());
    }

    @Test
    void cloturer_should_apply_ajustement_for_each_line_with_non_zero_ecart_and_pose_cloture() {
        inventaireEnCours.setStatut(InventaireStatut.BILAN);
        LigneInventaire surplus = ligne(5, 8);
        LigneInventaire neutre = ligne(10, 10);
        LigneInventaire manque = ligne(20, 18);
        Inventaire cloture = new Inventaire();
        cloture.setId(inventaireId);
        cloture.setMagasin(magasin);
        cloture.setStatut(InventaireStatut.CLOTURE);
        cloture.setDate(LocalDate.now());

        org.store.stock.domain.model.Stock stock = new org.store.stock.domain.model.Stock();
        stock.setMagasin(magasin);
        stock.setQuantiteDisponible(20);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);
        when(ligneInventaireService.findAllByInventaireId(inventaireId)).thenReturn(List.of(surplus, neutre, manque));
        when(stockService.findByMagasinAndProductFournisseur(eq(magasinId), any()))
                .thenReturn(Optional.of(stock));
        when(messageSourceService.getMessage(eq("inventaire.cloture.commentaire"), any(Object[].class))).thenReturn("Cloture " + inventaireId);
        when(inventaireDomainService.transitionStatut(inventaireEnCours, InventaireStatut.CLOTURE)).thenReturn(cloture);

        InventaireResponse response = service.cloturer(inventaireId, null);

        assertThat(response.statut()).isEqualTo(InventaireStatut.CLOTURE);
        ArgumentCaptor<AjustementStockRequest> captor = ArgumentCaptor.forClass(AjustementStockRequest.class);
        verify(ajustementStockService, times(2)).create(captor.capture());
        List<AjustementStockRequest> calls = captor.getAllValues();
        assertThat(calls).extracting(AjustementStockRequest::type)
                .containsExactly(TypeAjustement.POSITIF, TypeAjustement.NEGATIF);
        assertThat(calls).extracting(AjustementStockRequest::quantite).containsExactly(3, 2);
        assertThat(calls).allMatch(req -> req.motif() == MotifAjustement.INVENTAIRE_PHYSIQUE);
        assertThat(calls.getFirst().prixAchat()).isEqualTo(new BigDecimal("10.00"));
        assertThat(calls.get(1).prixAchat()).isNull();
        verify(rapportInventaireService, never()).create(any(), any());
    }

    @Test
    void reconcilierQuantitesTheoriques_should_update_stale_lines_and_skip_matching_ones() {
        UUID pfId1 = UUID.randomUUID();
        ProductFournisseur pf1 = new ProductFournisseur();
        pf1.setId(pfId1);
        pf1.setProduct(product);

        UUID pfId2 = UUID.randomUUID();
        ProductFournisseur pf2 = new ProductFournisseur();
        pf2.setId(pfId2);
        pf2.setProduct(product);

        LigneInventaire ligneStale = new LigneInventaire();
        ligneStale.setId(UUID.randomUUID());
        ligneStale.setInventaire(inventaireEnCours);
        ligneStale.setProductFournisseur(pf1);
        ligneStale.setQuantiteTheorique(9);
        ligneStale.setQuantiteReelle(0);
        ligneStale.setEcart(-9);

        LigneInventaire ligneOk = new LigneInventaire();
        ligneOk.setId(UUID.randomUUID());
        ligneOk.setInventaire(inventaireEnCours);
        ligneOk.setProductFournisseur(pf2);
        ligneOk.setQuantiteTheorique(5);
        ligneOk.setQuantiteReelle(5);
        ligneOk.setEcart(0);

        EntreeStock lot1 = lot(8);
        EntreeStock lot2 = lot(5);

        when(entreeStockService.findAvailableLotsForFifo(magasinId, pfId1)).thenReturn(List.of(lot1));
        when(entreeStockService.findAvailableLotsForFifo(magasinId, pfId2)).thenReturn(List.of(lot2));

        service.reconcilierQuantitesTheoriques(inventaireEnCours, List.of(ligneStale, ligneOk));

        verify(ligneInventaireService).updateQuantiteTheorique(ligneStale, 8);
        verify(ligneInventaireService, never()).updateQuantiteTheorique(eq(ligneOk), anyInt());
    }

    @Test
    void cloturer_should_throw_when_inventaire_not_bilan() {
        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);

        assertThatThrownBy(() -> service.cloturer(inventaireId, null))
                .isInstanceOf(BadArgumentException.class);

        verify(ajustementStockService, never()).create(any());
    }

    @Test
    void annuler_should_transition_from_en_cours_to_annule() {
        Inventaire annule = new Inventaire();
        annule.setId(inventaireId);
        annule.setMagasin(magasin);
        annule.setStatut(InventaireStatut.ANNULE);
        annule.setDate(LocalDate.now());

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);
        when(inventaireDomainService.transitionStatut(inventaireEnCours, InventaireStatut.ANNULE)).thenReturn(annule);

        assertThat(service.annuler(inventaireId).statut()).isEqualTo(InventaireStatut.ANNULE);
    }

    @Test
    void annuler_should_transition_from_bilan_to_annule() {
        inventaireEnCours.setStatut(InventaireStatut.BILAN);
        Inventaire annule = new Inventaire();
        annule.setId(inventaireId);
        annule.setMagasin(magasin);
        annule.setStatut(InventaireStatut.ANNULE);
        annule.setDate(LocalDate.now());

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);
        when(inventaireDomainService.transitionStatut(inventaireEnCours, InventaireStatut.ANNULE)).thenReturn(annule);

        assertThat(service.annuler(inventaireId).statut()).isEqualTo(InventaireStatut.ANNULE);
    }

    @Test
    void annuler_should_throw_when_inventaire_cloture() {
        inventaireEnCours.setStatut(InventaireStatut.CLOTURE);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);

        assertThatThrownBy(() -> service.annuler(inventaireId))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void findResponseById_should_throw_entity_exception_when_absent_or_other_entreprise() {
        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findResponseById(inventaireId, entrepriseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findResponseById(inventaireId))
                .isInstanceOf(EntityException.class);
    }

    @Test
    void findRapportByInventaireId_should_throw_when_absent() {
        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(rapportInventaireService.findResponseByInventaireId(inventaireId, entrepriseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findRapportByInventaireId(inventaireId))
                .isInstanceOf(EntityException.class);
    }

    @Test
    void create_should_create_inventaire_with_type_automatique() {
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(inventaireDomainService.hasActiveInventaire(magasinId)).thenReturn(false);

        Inventaire automatique = new Inventaire();
        automatique.setId(UUID.randomUUID());
        automatique.setMagasin(magasin);
        automatique.setType(TypeInventaire.AUTOMATIQUE);
        automatique.setStatut(InventaireStatut.EN_COURS);
        automatique.setDate(LocalDate.now());

        when(inventaireDomainService.create(eq(magasin), any(LocalDate.class), eq(TypeInventaire.AUTOMATIQUE)))
                .thenReturn(automatique);

        InventaireResponse response = service.create(magasinId, TypeInventaire.AUTOMATIQUE);

        assertThat(response.type()).isEqualTo(TypeInventaire.AUTOMATIQUE);
        assertThat(response.statut()).isEqualTo(InventaireStatut.EN_COURS);
    }

    @Test
    void addLigne_should_throw_when_type_automatique() {
        inventaireEnCours.setType(TypeInventaire.AUTOMATIQUE);
        LigneInventaireRequest request = new LigneInventaireRequest(productFournisseurId, 3, new BigDecimal("10.00"));

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);

        assertThatThrownBy(() -> service.addLigne(inventaireId, request))
                .isInstanceOf(BadArgumentException.class);

        verify(ligneInventaireService, never()).create(any(), any(), any(int.class), any(int.class), any());
    }

    @Test
    void passerEnBilan_automatique_should_use_computeValuation_and_set_ecart_zero() {
        inventaireEnCours.setType(TypeInventaire.AUTOMATIQUE);
        LocalDate dateDebut = LocalDate.now().minusDays(7);

        Inventaire bilan = new Inventaire();
        bilan.setId(inventaireId);
        bilan.setMagasin(magasin);
        bilan.setType(TypeInventaire.AUTOMATIQUE);
        bilan.setStatut(InventaireStatut.BILAN);
        bilan.setDate(LocalDate.now());

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);
        when(inventaireDomainService.transitionStatut(inventaireEnCours, InventaireStatut.BILAN)).thenReturn(bilan);
        when(stockService.computeValuation(magasin)).thenReturn(new StockValuationResponse(magasinId, new BigDecimal("5000.00"), 3L));
        when(depenseService.computeTotal(any())).thenReturn(null);

        InventaireResponse response = service.passerEnBilan(inventaireId, bilanRequest("500.00", "400.00", dateDebut));

        assertThat(response.statut()).isEqualTo(InventaireStatut.BILAN);
        ArgumentCaptor<RapportInventaireCommand> captor = ArgumentCaptor.forClass(RapportInventaireCommand.class);
        verify(rapportInventaireService).create(eq(bilan), captor.capture());
        RapportInventaireCommand command = captor.getValue();
        assertThat(command.montantAutomatique()).isEqualByComparingTo("5000.00");
        assertThat(command.montantPhysique()).isEqualByComparingTo("5000.00");
        verify(ligneInventaireService, never()).findAllByInventaireId(any());
    }

    @Test
    void cloturer_automatique_should_transition_without_stock_adjustments() {
        inventaireEnCours.setType(TypeInventaire.AUTOMATIQUE);
        inventaireEnCours.setStatut(InventaireStatut.BILAN);

        Inventaire cloture = new Inventaire();
        cloture.setId(inventaireId);
        cloture.setMagasin(magasin);
        cloture.setType(TypeInventaire.AUTOMATIQUE);
        cloture.setStatut(InventaireStatut.CLOTURE);
        cloture.setDate(LocalDate.now());

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(inventaireDomainService.findById(inventaireId)).thenReturn(inventaireEnCours);
        when(inventaireDomainService.transitionStatut(inventaireEnCours, InventaireStatut.CLOTURE)).thenReturn(cloture);

        InventaireResponse response = service.cloturer(inventaireId, null);

        assertThat(response.statut()).isEqualTo(InventaireStatut.CLOTURE);
        verify(ajustementStockService, never()).create(any());
        verify(ligneInventaireService, never()).findAllByInventaireId(any());
    }

    @Test
    void findAllByCurrentEntreprise_should_delegate_filter_to_domain_service() {
        InventaireFilter filter = new InventaireFilter(magasinId, null, null, null, null, 0, 10);
        InventaireResponse response = new InventaireResponse(inventaireEnCours);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(inventaireDomainService.findResponsesByFilter(filter, entrepriseId))
                .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 10), 1));

        assertThat(service.findAllByCurrentEntreprise(filter).getContent()).hasSize(1);
    }
}
