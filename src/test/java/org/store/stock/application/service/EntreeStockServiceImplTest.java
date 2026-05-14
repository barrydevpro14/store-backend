package org.store.stock.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.exceptions.ForbiddenException;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.application.dto.EntreeStockResponse;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.service.impl.EntreeStockServiceImpl;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.EntreeStockDomainService;
import org.store.stock.domain.service.MouvementStockDomainService;
import org.store.stock.domain.service.StockDomainService;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class EntreeStockServiceImplTest {

    @Mock private EntreeStockDomainService entreeStockDomainService;
    @Mock private StockDomainService stockDomainService;
    @Mock private MouvementStockDomainService mouvementStockDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private IProductFournisseurService productFournisseurService;

    @InjectMocks
    private EntreeStockServiceImpl service;

    private UUID magasinId;
    private UUID productFournisseurId;
    private UUID productId;
    private UUID entrepriseId;

    private Magasin magasin;
    private Product produit;
    private Fournisseur fournisseur;
    private ProductFournisseur productFournisseur;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();
        productId = UUID.randomUUID();

        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setNom("Magasin Central");
        magasin.setEntreprise(entreprise);

        produit = new Product();
        produit.setId(productId);
        produit.setNom("Clou 10mm");
        produit.setReference("CL-10");
        produit.setEntreprise(entreprise);

        fournisseur = new Fournisseur();
        fournisseur.setId(UUID.randomUUID());
        fournisseur.setNom("Fournisseur Chine");
        fournisseur.setEntreprise(entreprise);

        productFournisseur = new ProductFournisseur();
        productFournisseur.setId(productFournisseurId);
        productFournisseur.setProduct(produit);
        productFournisseur.setFournisseur(fournisseur);
        productFournisseur.setPrixAchat(new BigDecimal("10.00"));
    }

    private EntreeStockRequest sampleRequest(int qty, BigDecimal prix) {
        return new EntreeStockRequest(magasinId, productFournisseurId, qty, prix, "LOT-001", LocalDate.now().plusYears(1), "achat manuel");
    }

    private EntreeStock sampleSaved(int qty, BigDecimal prix) {
        EntreeStock entreeStock = new EntreeStock();
        entreeStock.setId(UUID.randomUUID());
        entreeStock.setMagasin(magasin);
        entreeStock.setProduit(produit);
        entreeStock.setProductFournisseur(productFournisseur);
        entreeStock.setQuantiteInitiale(qty);
        entreeStock.setQuantiteRestante(qty);
        entreeStock.setPrixAchat(prix);
        entreeStock.setNumeroLot("LOT-001");
        entreeStock.setDateExpiration(LocalDate.now().plusYears(1));
        return entreeStock;
    }

    @Test
    void create_should_create_lot_upsert_stock_and_journalize_when_stock_absent() {
        EntreeStockRequest request = sampleRequest(100, new BigDecimal("10.00"));
        EntreeStock savedLot = sampleSaved(100, new BigDecimal("10.00"));
        Stock upsertedStock = new Stock();
        upsertedStock.setId(UUID.randomUUID());
        upsertedStock.setMagasin(magasin);
        upsertedStock.setProduit(produit);
        upsertedStock.setQuantiteDisponible(100);
        upsertedStock.setPrixAchatMoyen(new BigDecimal("10.00"));

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(stockDomainService.findByMagasinIdAndProduitId(magasinId, productId)).thenReturn(Optional.empty());
        when(entreeStockDomainService.create(eq(request), eq(magasin), eq(produit), eq(productFournisseur))).thenReturn(savedLot);
        when(stockDomainService.createOrUpdateEntry(magasin, produit, 100, new BigDecimal("10.00"))).thenReturn(upsertedStock);

        EntreeStockResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(savedLot.getId());
        assertThat(response.magasin().id()).isEqualTo(magasinId);
        assertThat(response.produit().id()).isEqualTo(productId);
        assertThat(response.fournisseur().id()).isEqualTo(fournisseur.getId());
        assertThat(response.quantiteInitiale()).isEqualTo(100);
        assertThat(response.quantiteRestante()).isEqualTo(100);
        assertThat(response.prixAchat()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(response.numeroLot()).isEqualTo("LOT-001");

        verify(mouvementStockDomainService).journalize(
                eq(upsertedStock),
                eq(new MouvementJournalize(MouvementStockType.ENTREE_ACHAT, 100, 0, 100, "LOT-001", "achat manuel"))
        );
    }

    @Test
    void create_should_journalize_with_stockAvant_from_existing_stock() {
        EntreeStockRequest request = sampleRequest(50, new BigDecimal("20.00"));
        EntreeStock savedLot = sampleSaved(50, new BigDecimal("20.00"));
        Stock existingStock = new Stock();
        existingStock.setId(UUID.randomUUID());
        existingStock.setMagasin(magasin);
        existingStock.setProduit(produit);
        existingStock.setQuantiteDisponible(100);
        existingStock.setPrixAchatMoyen(new BigDecimal("10.00"));
        Stock upsertedStock = new Stock();
        upsertedStock.setId(existingStock.getId());
        upsertedStock.setMagasin(magasin);
        upsertedStock.setProduit(produit);
        upsertedStock.setQuantiteDisponible(150);
        upsertedStock.setPrixAchatMoyen(new BigDecimal("13.33"));

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(stockDomainService.findByMagasinIdAndProduitId(magasinId, productId)).thenReturn(Optional.of(existingStock));
        when(entreeStockDomainService.create(eq(request), eq(magasin), eq(produit), eq(productFournisseur))).thenReturn(savedLot);
        when(stockDomainService.createOrUpdateEntry(magasin, produit, 50, new BigDecimal("20.00"))).thenReturn(upsertedStock);

        service.create(request);

        ArgumentCaptor<MouvementJournalize> captor = ArgumentCaptor.forClass(MouvementJournalize.class);
        verify(mouvementStockDomainService).journalize(eq(upsertedStock), captor.capture());
        MouvementJournalize captured = captor.getValue();
        assertThat(captured.type()).isEqualTo(MouvementStockType.ENTREE_ACHAT);
        assertThat(captured.quantite()).isEqualTo(50);
        assertThat(captured.stockAvant()).isEqualTo(100);
        assertThat(captured.stockApres()).isEqualTo(150);
    }

    @Test
    void create_should_propagate_forbidden_when_magasin_not_accessible() {
        EntreeStockRequest request = sampleRequest(10, new BigDecimal("10.00"));

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ForbiddenException.class);

        verify(entreeStockDomainService, never()).create(any(), any(), any(), any());
        verify(stockDomainService, never()).createOrUpdateEntry(any(), any(), anyInt(), any());
        verify(mouvementStockDomainService, never()).journalize(any(), any());
    }

    @Test
    void create_should_propagate_forbidden_when_productFournisseur_belongs_to_other_entreprise() {
        EntreeStockRequest request = sampleRequest(10, new BigDecimal("10.00"));

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur))
                .thenThrow(new ForbiddenException("productFournisseur.notOwned"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ForbiddenException.class);

        verify(entreeStockDomainService, never()).create(any(), any(), any(), any());
        verify(stockDomainService, never()).createOrUpdateEntry(any(), any(), anyInt(), any());
    }
}
