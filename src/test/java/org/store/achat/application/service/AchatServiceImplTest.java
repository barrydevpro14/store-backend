package org.store.achat.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.application.dto.AchatDraftResponse;
import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatResponse;
import org.store.achat.application.dto.AchatValidateRequest;
import org.store.achat.application.dto.AnnulationAchatRequest;
import org.store.achat.application.dto.AnnulationAchatResponse;
import org.store.achat.application.dto.CommandeAchatCreate;
import org.store.achat.application.dto.FactureAchatCreate;
import org.store.achat.application.dto.FactureAchatCreateRequest;
import org.store.achat.application.dto.LigneAchatRequest;
import org.store.achat.application.dto.LigneAchatUpdateRequest;
import org.store.achat.application.dto.LigneCommandeAchatCreate;
import org.store.achat.application.dto.LigneCommandeAchatResponse;
import org.store.achat.application.service.impl.AchatServiceImpl;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.enums.MotifAnnulationAchat;
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
import org.store.stock.application.dto.EntreeStockCreate;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.domain.enums.MouvementStockType;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AchatServiceImplTest {

    @Mock private CommandeAchatDomainService commandeAchatDomainService;
    @Mock private LigneCommandeAchatDomainService ligneCommandeAchatDomainService;
    @Mock private FactureAchatDomainService factureAchatDomainService;
    @Mock private EntreeStockDomainService entreeStockDomainService;
    @Mock private StockDomainService stockDomainService;
    @Mock private MouvementStockDomainService mouvementStockDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private IFournisseurService fournisseurService;
    @Mock private IProductFournisseurService productFournisseurService;
    @Mock private org.store.achat.application.service.ICommandeAchatService commandeAchatService;
    @Mock private ValidatorService validatorService;
    @Mock private PurchaseProperties purchaseProperties;

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

        productFournisseur = new ProductFournisseur();
        productFournisseur.setId(productFournisseurId);
        productFournisseur.setProduct(produit);
        productFournisseur.setFournisseur(fournisseur);
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
                List.of(new LigneAchatRequest(productFournisseurId, 100, new BigDecimal("10.00"), new BigDecimal("15.00"), "LOT-1", null))
        );
    }

    private AchatValidateRequest sampleValidateRequest() {
        return new AchatValidateRequest(new FactureAchatCreateRequest("FAC-001", LocalDate.of(2026, 5, 15), null));
    }

    private LigneCommandeAchat sampleLigne(int quantite, BigDecimal prixAchat, BigDecimal prixVente) {
        LigneCommandeAchat ligne = new LigneCommandeAchat();
        ligne.setId(ligneId);
        ligne.setCommande(commande);
        ligne.setProductFournisseur(productFournisseur);
        ligne.setQuantite(quantite);
        ligne.setPrixAchat(prixAchat);
        ligne.setPrixVente(prixVente);
        return ligne;
    }

    @Test
    void create_should_persist_draft_without_stock_or_facture() {
        AchatRequest req = sampleRequest();

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(fournisseurService.findById(fournisseurId)).thenReturn(fournisseur);
        when(fournisseurService.ensureBelongsToCurrentEntreprise(fournisseur)).thenReturn(fournisseur);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(commandeAchatDomainService.generateReference()).thenReturn("CMD-AUTO");
        when(commandeAchatDomainService.create(any(CommandeAchatCreate.class))).thenReturn(commande);
        when(commandeAchatDomainService.findById(commande.getId())).thenReturn(commande);
        when(ligneCommandeAchatDomainService.create(any(LigneCommandeAchatCreate.class))).thenReturn(new LigneCommandeAchat());

        AchatDraftResponse response = service.create(req);

        assertThat(response.commande().reference()).isEqualTo("CMD-AUTO");
        assertThat(response.commande().statut()).isEqualTo(CommandeAchatStatut.DRAFT);

        ArgumentCaptor<CommandeAchatCreate> captor = ArgumentCaptor.forClass(CommandeAchatCreate.class);
        verify(commandeAchatDomainService).create(captor.capture());
        assertThat(captor.getValue().statut()).isEqualTo(CommandeAchatStatut.DRAFT);

        verify(factureAchatDomainService, never()).create(any());
        verify(entreeStockDomainService, never()).create(any());
        verify(mouvementStockDomainService, never()).journalize(any(), any());
        verify(productFournisseurService, never()).applyPrixVenteFromPurchase(any(), any());
    }

    @Test
    void create_should_throw_when_productFournisseur_does_not_belong_to_supplier() {
        Fournisseur autreFournisseur = new Fournisseur();
        autreFournisseur.setId(UUID.randomUUID());
        productFournisseur.setFournisseur(autreFournisseur);

        AchatRequest req = sampleRequest();

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(fournisseurService.findById(fournisseurId)).thenReturn(fournisseur);
        when(fournisseurService.ensureBelongsToCurrentEntreprise(fournisseur)).thenReturn(fournisseur);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadArgumentException.class);

        verify(commandeAchatDomainService, never()).create(any(CommandeAchatCreate.class));
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
    void validate_should_create_facture_and_switch_to_validee_without_stock() {
        LigneCommandeAchat ligne = sampleLigne(100, new BigDecimal("10.00"), new BigDecimal("15.00"));
        commande.setLignes(List.of(ligne));

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(factureAchatDomainService.create(any(FactureAchatCreate.class))).thenReturn(facture);
        when(commandeAchatDomainService.validate(commande)).thenAnswer(inv -> {
            commande.setStatut(CommandeAchatStatut.VALIDEE);
            return commande;
        });

        AchatResponse response = service.validate(commandeId, sampleValidateRequest());

        assertThat(response.commande().statut()).isEqualTo(CommandeAchatStatut.VALIDEE);
        assertThat(response.facture().numero()).isEqualTo("FAC-001");

        ArgumentCaptor<FactureAchatCreate> factureCaptor = ArgumentCaptor.forClass(FactureAchatCreate.class);
        verify(factureAchatDomainService).create(factureCaptor.capture());
        assertThat(factureCaptor.getValue().montantTotal()).isEqualByComparingTo(new BigDecimal("1000.00"));

        verify(entreeStockDomainService, never()).create(any(EntreeStockCreate.class));
        verify(mouvementStockDomainService, never()).journalize(any(), any());
        verify(productFournisseurService, never()).applyPrixVenteFromPurchase(any(), any());
    }

    @Test
    void validate_should_compute_total_from_lines() {
        LigneCommandeAchat l1 = sampleLigne(100, new BigDecimal("10.00"), new BigDecimal("15.00"));
        LigneCommandeAchat l2 = sampleLigne(50, new BigDecimal("15.00"), new BigDecimal("20.00"));
        l2.setId(UUID.randomUUID());
        commande.setLignes(List.of(l1, l2));

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(factureAchatDomainService.create(any(FactureAchatCreate.class))).thenReturn(facture);
        when(commandeAchatDomainService.validate(commande)).thenReturn(commande);

        service.validate(commandeId, sampleValidateRequest());

        ArgumentCaptor<FactureAchatCreate> captor = ArgumentCaptor.forClass(FactureAchatCreate.class);
        verify(factureAchatDomainService).create(captor.capture());
        assertThat(captor.getValue().montantTotal()).isEqualByComparingTo(new BigDecimal("1750.00"));
    }

    @Test
    void validate_should_throw_when_commande_not_draft() {
        commande.setStatut(CommandeAchatStatut.RECEPTIONNEE);

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);

        assertThatThrownBy(() -> service.validate(commandeId, sampleValidateRequest()))
                .isInstanceOf(BadArgumentException.class);

        verify(factureAchatDomainService, never()).create(any());
        verify(commandeAchatDomainService, never()).validate(any());
    }

    @Test
    void validate_should_propagate_forbidden_when_not_owned() {
        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande))
                .thenThrow(new ForbiddenException("commandeAchat.notOwned"));

        assertThatThrownBy(() -> service.validate(commandeId, sampleValidateRequest()))
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
        when(ligneCommandeAchatDomainService.update(eq(ligne), eq(150), eq(new BigDecimal("12.00")), eq(new BigDecimal("18.00")), eq("LOT-2"), eq(null)))
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

        verify(ligneCommandeAchatDomainService, never()).update(any(), any(int.class), any(), any(), any(), any());
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

        verify(ligneCommandeAchatDomainService, never()).update(any(), any(int.class), any(), any(), any(), any());
    }

    @Test
    void deleteLigne_should_remove_ligne_when_draft_and_not_last() {
        LigneCommandeAchat ligne1 = sampleLigne(100, new BigDecimal("10.00"), new BigDecimal("15.00"));
        LigneCommandeAchat ligne2 = sampleLigne(50, new BigDecimal("12.00"), new BigDecimal("18.00"));
        ligne2.setId(UUID.randomUUID());
        commande.setLignes(List.of(ligne1, ligne2));

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
    void findDetailsById_should_return_commande_facture_and_lignes() {
        LigneCommandeAchat ligne = sampleLigne(10, new BigDecimal("10.00"), new BigDecimal("15.00"));
        commande.setLignes(List.of(ligne));

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(factureAchatDomainService.findByCommandeId(commandeId)).thenReturn(Optional.of(facture));

        org.store.achat.application.dto.AchatDetailsResponse response = service.findDetailsById(commandeId);

        assertThat(response.commande().reference()).isEqualTo("CMD-AUTO");
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
        when(entreeStockDomainService.findByCommandeAchatId(commandeId)).thenReturn(List.of(lot));
        when(stockDomainService.findByMagasinIdAndProduitId(magasinId, produit.getId())).thenReturn(Optional.of(stock));
        when(stockDomainService.decrement(stock, 100)).thenAnswer(inv -> {
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

        verify(entreeStockDomainService).markAsAnnulee(lot);
        verify(factureAchatDomainService).cancel(facture);

        ArgumentCaptor<MouvementJournalize> mouvementCaptor = ArgumentCaptor.forClass(MouvementJournalize.class);
        verify(mouvementStockDomainService).journalize(eq(stock), mouvementCaptor.capture());
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

        assertThatThrownBy(() -> service.cancel(commandeId, new AnnulationAchatRequest("ERREUR_SAISIE", null)))
                .isInstanceOf(BadArgumentException.class)
                .hasMessageContaining("alreadyCancelled");

        verify(entreeStockDomainService, never()).findByCommandeAchatId(any());
        verify(commandeAchatDomainService, never()).cancel(any(), any(), any());
    }

    @Test
    void cancel_should_throw_when_statut_is_draft() {
        commande.setStatut(CommandeAchatStatut.DRAFT);
        commande.setCreatedAt(LocalDateTime.now().minusHours(1));

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);

        assertThatThrownBy(() -> service.cancel(commandeId, new AnnulationAchatRequest("ERREUR_SAISIE", null)))
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

        assertThatThrownBy(() -> service.cancel(commandeId, new AnnulationAchatRequest("ERREUR_SAISIE", null)))
                .isInstanceOf(BadArgumentException.class)
                .hasMessageContaining("windowExpired");

        verify(entreeStockDomainService, never()).findByCommandeAchatId(any());
    }

    @Test
    void cancel_should_propagate_forbidden_when_not_owned() {
        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande))
                .thenThrow(new ForbiddenException("commandeAchat.notOwned"));

        assertThatThrownBy(() -> service.cancel(commandeId, new AnnulationAchatRequest("ERREUR_SAISIE", null)))
                .isInstanceOf(ForbiddenException.class);

        verify(entreeStockDomainService, never()).findByCommandeAchatId(any());
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
        when(entreeStockDomainService.findByCommandeAchatId(commandeId)).thenReturn(List.of(lotIntact, lotConsomme));

        assertThatThrownBy(() -> service.cancel(commandeId, new AnnulationAchatRequest("ERREUR_SAISIE", null)))
                .isInstanceOf(BadArgumentException.class)
                .hasMessageContaining("lotAlreadyConsumed");

        verify(stockDomainService, never()).decrement(any(), any(int.class));
        verify(entreeStockDomainService, never()).markAsAnnulee(any());
        verify(commandeAchatDomainService, never()).cancel(any(), any(), any());
    }
}
