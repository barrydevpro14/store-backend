package org.store.stock.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.application.service.IProductService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.stock.application.dto.AjustementStockRequest;
import org.store.stock.application.dto.EntreeStockCreate;
import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.StockEntryContext;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.stock.application.service.impl.AjustementStockServiceImpl;
import org.store.stock.domain.enums.MotifAjustement;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.enums.TypeAjustement;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.MouvementStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.EntreeStockDomainService;
import org.store.stock.domain.service.MouvementStockDomainService;
import org.store.stock.domain.service.StockDomainService;

import java.math.BigDecimal;
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
class AjustementStockServiceImplTest {

    @Mock private EntreeStockDomainService entreeStockDomainService;
    @Mock private StockDomainService stockDomainService;
    @Mock private MouvementStockDomainService mouvementStockDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private IProductService productService;
    @Mock private IProductFournisseurService productFournisseurService;

    @InjectMocks
    private AjustementStockServiceImpl service;

    private UUID magasinId;
    private UUID productId;
    private UUID productFournisseurId;
    private Entreprise entreprise;
    private Magasin magasin;
    private Product produit;
    private Fournisseur fournisseur;
    private ProductFournisseur productFournisseur;
    private Stock stock;

    @BeforeEach
    void setUp() {
        magasinId = UUID.randomUUID();
        productId = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(UUID.randomUUID());

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);

        produit = new Product();
        produit.setId(productId);
        produit.setEntreprise(entreprise);

        fournisseur = new Fournisseur();
        fournisseur.setId(UUID.randomUUID());
        fournisseur.setNom("Fournisseur Chine");

        productFournisseur = new ProductFournisseur();
        productFournisseur.setId(productFournisseurId);
        productFournisseur.setProduct(produit);
        productFournisseur.setFournisseur(fournisseur);

        stock = new Stock();
        stock.setId(UUID.randomUUID());
        stock.setMagasin(magasin);
        stock.setProduit(produit);
        stock.setQuantiteDisponible(100);
    }

    private AjustementStockRequest positifRequest(int qty, BigDecimal prixAchat, MotifAjustement motif) {
        return new AjustementStockRequest(magasinId, productId, TypeAjustement.POSITIF, qty,
                productFournisseurId, prixAchat, motif, "retrouvaille");
    }

    private AjustementStockRequest negatifRequest(int qty, MotifAjustement motif) {
        return new AjustementStockRequest(magasinId, productId, TypeAjustement.NEGATIF, qty,
                null, null, motif, "perte rayon");
    }

    private MouvementStock buildMouvement() {
        MouvementStock m = new MouvementStock();
        m.setId(UUID.randomUUID());
        m.setStock(stock);
        m.setType(MouvementStockType.AJUSTEMENT);
        return m;
    }

    @Test
    void create_positif_should_create_lot_and_upsert_stock() {
        AjustementStockRequest req = positifRequest(20, new BigDecimal("10.00"), MotifAjustement.RETROUVAILLE);
        Stock updated = new Stock();
        updated.setQuantiteDisponible(120);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productService.findById(productId)).thenReturn(produit);
        when(productService.ensureBelongsToCurrentEntreprise(produit)).thenReturn(produit);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(entreeStockDomainService.create(any(EntreeStockCreate.class)))
                .thenReturn(new EntreeStock());
        when(stockDomainService.createOrUpdateEntry(any(StockEntryContext.class)))
                .thenReturn(updated);
        when(mouvementStockDomainService.journalize(eq(updated), any(MouvementJournalize.class))).thenReturn(buildMouvement());

        MouvementStockResponse response = service.create(req);

        assertThat(response.detail().type()).isEqualTo(MouvementStockType.AJUSTEMENT);
        verify(entreeStockDomainService).create(any(EntreeStockCreate.class));
    }

    @Test
    void create_positif_should_throw_when_productFournisseurId_missing() {
        AjustementStockRequest req = new AjustementStockRequest(magasinId, productId, TypeAjustement.POSITIF, 20,
                null, new BigDecimal("10.00"), MotifAjustement.RETROUVAILLE, null);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productService.findById(productId)).thenReturn(produit);
        when(productService.ensureBelongsToCurrentEntreprise(produit)).thenReturn(produit);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void create_positif_should_throw_when_productFournisseur_does_not_match_product() {
        Product autreProduit = new Product();
        autreProduit.setId(UUID.randomUUID());
        autreProduit.setEntreprise(entreprise);
        productFournisseur.setProduct(autreProduit);

        AjustementStockRequest req = positifRequest(20, new BigDecimal("10.00"), MotifAjustement.RETROUVAILLE);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productService.findById(productId)).thenReturn(produit);
        when(productService.ensureBelongsToCurrentEntreprise(produit)).thenReturn(produit);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void create_negatif_should_consume_lots_fifo_without_sortie() {
        AjustementStockRequest req = negatifRequest(30, MotifAjustement.CASSE);
        EntreeStock l1 = new EntreeStock();
        l1.setId(UUID.randomUUID());
        l1.setQuantiteRestante(50);
        Stock updated = new Stock();
        updated.setQuantiteDisponible(70);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productService.findById(productId)).thenReturn(produit);
        when(productService.ensureBelongsToCurrentEntreprise(produit)).thenReturn(produit);
        when(stockDomainService.findByMagasinIdAndProduitId(magasinId, productId)).thenReturn(Optional.of(stock));
        when(entreeStockDomainService.findAvailableLotsForFifo(magasinId, productId)).thenReturn(List.of(l1));
        when(entreeStockDomainService.save(any(EntreeStock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockDomainService.decrement(stock, 30)).thenReturn(updated);
        when(mouvementStockDomainService.journalize(eq(updated), any(MouvementJournalize.class))).thenReturn(buildMouvement());

        service.create(req);

        assertThat(l1.getQuantiteRestante()).isEqualTo(20);
        verify(stockDomainService).decrement(stock, 30);
    }

    @Test
    void create_negatif_should_throw_when_stock_not_found() {
        AjustementStockRequest req = negatifRequest(30, MotifAjustement.PERTE);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productService.findById(productId)).thenReturn(produit);
        when(productService.ensureBelongsToCurrentEntreprise(produit)).thenReturn(produit);
        when(stockDomainService.findByMagasinIdAndProduitId(magasinId, productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(EntityException.class);
    }

    @Test
    void create_negatif_should_throw_when_insufficient_quantity() {
        stock.setQuantiteDisponible(10);
        AjustementStockRequest req = negatifRequest(30, MotifAjustement.PERTE);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productService.findById(productId)).thenReturn(produit);
        when(productService.ensureBelongsToCurrentEntreprise(produit)).thenReturn(produit);
        when(stockDomainService.findByMagasinIdAndProduitId(magasinId, productId)).thenReturn(Optional.of(stock));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadArgumentException.class);

        verify(stockDomainService, never()).decrement(any(), anyInt());
    }

    @Test
    void create_should_throw_when_motif_VOL_with_type_POSITIF() {
        AjustementStockRequest req = new AjustementStockRequest(magasinId, productId, TypeAjustement.POSITIF, 20,
                productFournisseurId, new BigDecimal("10.00"), MotifAjustement.VOL, null);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadArgumentException.class);

        verify(magasinService, never()).findById(any());
    }

    @Test
    void create_should_throw_when_motif_RETROUVAILLE_with_type_NEGATIF() {
        AjustementStockRequest req = new AjustementStockRequest(magasinId, productId, TypeAjustement.NEGATIF, 20,
                null, null, MotifAjustement.RETROUVAILLE, null);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void create_negatif_should_journalize_with_correct_quantities() {
        stock.setQuantiteDisponible(100);
        AjustementStockRequest req = negatifRequest(30, MotifAjustement.CASSE);
        EntreeStock l1 = new EntreeStock();
        l1.setQuantiteRestante(50);
        Stock updated = new Stock();
        updated.setQuantiteDisponible(70);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productService.findById(productId)).thenReturn(produit);
        when(productService.ensureBelongsToCurrentEntreprise(produit)).thenReturn(produit);
        when(stockDomainService.findByMagasinIdAndProduitId(magasinId, productId)).thenReturn(Optional.of(stock));
        when(entreeStockDomainService.findAvailableLotsForFifo(magasinId, productId)).thenReturn(List.of(l1));
        when(entreeStockDomainService.save(any(EntreeStock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stockDomainService.decrement(stock, 30)).thenReturn(updated);
        when(mouvementStockDomainService.journalize(eq(updated), any(MouvementJournalize.class))).thenReturn(buildMouvement());

        service.create(req);

        ArgumentCaptor<MouvementJournalize> captor = ArgumentCaptor.forClass(MouvementJournalize.class);
        verify(mouvementStockDomainService).journalize(eq(updated), captor.capture());
        MouvementJournalize captured = captor.getValue();
        assertThat(captured.type()).isEqualTo(MouvementStockType.AJUSTEMENT);
        assertThat(captured.quantite()).isEqualTo(30);
        assertThat(captured.stockAvant()).isEqualTo(100);
        assertThat(captured.stockApres()).isEqualTo(70);
        assertThat(captured.referenceDocument()).isEqualTo("CASSE");
    }
}
