package org.store.achat.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatResponse;
import org.store.achat.application.dto.CommandeAchatCreate;
import org.store.achat.application.dto.FactureAchatCreate;
import org.store.achat.application.dto.FactureAchatCreateRequest;
import org.store.achat.application.dto.LigneAchatRequest;
import org.store.achat.application.dto.LigneCommandeAchatCreate;
import org.store.achat.application.service.impl.AchatServiceImpl;
import org.store.achat.domain.enums.CommandeAchatStatut;
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
    @Mock private org.store.achat.application.service.IFactureAchatService factureAchatService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private AchatServiceImpl service;

    private UUID magasinId;
    private UUID fournisseurId;
    private UUID productFournisseurId;
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
        commande.setId(UUID.randomUUID());
        commande.setReference("CMD-AUTO");
        commande.setStatut(CommandeAchatStatut.RECEPTIONNEE);
        commande.setMagasin(magasin);
        commande.setFournisseur(fournisseur);
        commande.setLignes(List.of());

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
                List.of(new LigneAchatRequest(productFournisseurId, 100, new BigDecimal("10.00"), new BigDecimal("15.00"), "LOT-1", null)),
                new FactureAchatCreateRequest("FAC-001", LocalDate.of(2026, 5, 15), null)
        );
    }

    @Test
    void create_should_orchestrate_purchase_flow() {
        AchatRequest req = sampleRequest();
        EntreeStock entree = new EntreeStock();
        entree.setId(UUID.randomUUID());
        Stock stock = new Stock();
        stock.setQuantiteDisponible(100);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(fournisseurService.findById(fournisseurId)).thenReturn(fournisseur);
        when(fournisseurService.ensureBelongsToCurrentEntreprise(fournisseur)).thenReturn(fournisseur);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(commandeAchatDomainService.generateReference()).thenReturn("CMD-AUTO");
        when(commandeAchatDomainService.create(any(CommandeAchatCreate.class))).thenReturn(commande);
        when(ligneCommandeAchatDomainService.create(any(LigneCommandeAchatCreate.class))).thenReturn(new LigneCommandeAchat());
        when(factureAchatDomainService.create(any(FactureAchatCreate.class))).thenReturn(facture);
        when(stockDomainService.findByMagasinIdAndProduitId(any(), any())).thenReturn(Optional.empty());
        when(entreeStockDomainService.create(any(EntreeStockCreate.class))).thenReturn(entree);
        when(stockDomainService.createOrUpdateEntry(eq(magasin), eq(produit), eq(100), eq(new BigDecimal("10.00"))))
                .thenReturn(stock);

        AchatResponse response = service.create(req);

        assertThat(response.commande().reference()).isEqualTo("CMD-AUTO");
        assertThat(response.facture().numero()).isEqualTo("FAC-001");
        verify(mouvementStockDomainService).journalize(eq(stock), any(MouvementJournalize.class));
    }

    @Test
    void create_should_compute_total_amount_correctly() {
        AchatRequest req = new AchatRequest(
                magasinId, fournisseurId, LocalDate.of(2026, 5, 15),
                List.of(
                        new LigneAchatRequest(productFournisseurId, 100, new BigDecimal("10.00"), new BigDecimal("15.00"), null, null),
                        new LigneAchatRequest(productFournisseurId, 50, new BigDecimal("15.00"), new BigDecimal("20.00"), null, null)
                ),
                new FactureAchatCreateRequest("FAC-001", LocalDate.of(2026, 5, 15), null)
        );
        Stock stock = new Stock();
        stock.setQuantiteDisponible(0);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(fournisseurService.findById(fournisseurId)).thenReturn(fournisseur);
        when(fournisseurService.ensureBelongsToCurrentEntreprise(fournisseur)).thenReturn(fournisseur);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(commandeAchatDomainService.generateReference()).thenReturn("CMD-AUTO");
        when(commandeAchatDomainService.create(any(CommandeAchatCreate.class))).thenReturn(commande);
        when(ligneCommandeAchatDomainService.create(any(LigneCommandeAchatCreate.class))).thenReturn(new LigneCommandeAchat());
        when(factureAchatDomainService.create(any(FactureAchatCreate.class))).thenReturn(facture);
        when(stockDomainService.findByMagasinIdAndProduitId(any(), any())).thenReturn(Optional.empty());
        when(entreeStockDomainService.create(any(EntreeStockCreate.class))).thenReturn(new EntreeStock());
        when(stockDomainService.createOrUpdateEntry(any(), any(), any(int.class), any())).thenReturn(stock);

        service.create(req);

        ArgumentCaptor<FactureAchatCreate> captor = ArgumentCaptor.forClass(FactureAchatCreate.class);
        verify(factureAchatDomainService).create(captor.capture());
        assertThat(captor.getValue().montantTotal()).isEqualByComparingTo(new BigDecimal("1750.00"));
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
    void findDetailsById_should_return_commande_facture_and_lignes() {
        UUID commandeId = commande.getId();
        org.store.achat.domain.model.LigneCommandeAchat ligne = new org.store.achat.domain.model.LigneCommandeAchat();
        ligne.setId(UUID.randomUUID());
        ligne.setQuantite(10);
        ligne.setPrixAchat(new BigDecimal("10.00"));
        ligne.setPrixVente(new BigDecimal("15.00"));
        ligne.setProductFournisseur(productFournisseur);
        commande.setLignes(java.util.List.of(ligne));

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande)).thenReturn(commande);
        when(factureAchatService.findByCommandeId(commandeId)).thenReturn(facture);

        org.store.achat.application.dto.AchatDetailsResponse response = service.findDetailsById(commandeId);

        assertThat(response.commande().reference()).isEqualTo("CMD-AUTO");
        assertThat(response.facture().numero()).isEqualTo("FAC-001");
        assertThat(response.lignes()).hasSize(1);
        assertThat(response.lignes().get(0).quantite()).isEqualTo(10);
        assertThat(response.lignes().get(0).prixAchat()).isEqualByComparingTo("10.00");
        assertThat(response.lignes().get(0).prixVente()).isEqualByComparingTo("15.00");
    }

    @Test
    void findDetailsById_should_throw_when_commande_not_owned() {
        UUID commandeId = commande.getId();

        when(commandeAchatService.findById(commandeId)).thenReturn(commande);
        when(commandeAchatService.ensureBelongsToCurrentEntreprise(commande))
                .thenThrow(new ForbiddenException("commandeAchat.notOwned"));

        assertThatThrownBy(() -> service.findDetailsById(commandeId))
                .isInstanceOf(ForbiddenException.class);

        verify(factureAchatService, never()).findByCommandeId(any());
    }
}
