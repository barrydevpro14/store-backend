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
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.stock.application.dto.AjustementStockRequest;
import org.store.stock.application.dto.EntreeStockCreate;
import org.store.stock.application.dto.MouvementDetailResponse;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.stock.application.dto.StockEntryContext;
import org.store.stock.application.service.impl.AjustementStockServiceImpl;
import org.store.stock.domain.enums.MotifAjustement;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.enums.TypeAjustement;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.Stock;

import java.math.BigDecimal;
import java.util.List;
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
class AjustementStockServiceImplTest {

    @Mock private IEntreeStockService entreeStockService;
    @Mock private IStockService stockService;
    @Mock private IMouvementStockService mouvementStockService;
    @Mock private IMagasinService magasinService;
    @Mock private org.store.security.application.service.ICurrentUserService currentUserService;
    @Mock private org.store.audit.application.service.IAuditEventPublisher auditEventPublisher;

    @InjectMocks
    private AjustementStockServiceImpl service;

    private UUID stockId;
    private UUID magasinId;
    private Magasin magasin;
    private ProductFournisseur productFournisseur;
    private Stock stock;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserService.getCurrent()).thenReturn(new org.store.security.application.dto.UserPrincipal(
                UUID.randomUUID(), null, UUID.randomUUID(), null, "test", null, null, "OWNER", List.of()));

        stockId = UUID.randomUUID();
        magasinId = UUID.randomUUID();

        Entreprise entreprise = new Entreprise();
        entreprise.setId(UUID.randomUUID());

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);

        Product produit = new Product();
        produit.setId(UUID.randomUUID());
        produit.setNom("Clou 10mm");
        produit.setEntreprise(entreprise);

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(UUID.randomUUID());
        fournisseur.setNom("Fournisseur Chine");

        productFournisseur = new ProductFournisseur();
        productFournisseur.setId(UUID.randomUUID());
        productFournisseur.setProduct(produit);
        productFournisseur.setFournisseur(fournisseur);
        productFournisseur.setPrixAchat(new BigDecimal("10.00"));

        stock = new Stock();
        stock.setId(stockId);
        stock.setMagasin(magasin);
        stock.setProductFournisseur(productFournisseur);
        stock.setQuantiteDisponible(new BigDecimal("100"));
    }

    private AjustementStockRequest positifRequest(int qty, MotifAjustement motif) {
        return new AjustementStockRequest(stockId, TypeAjustement.POSITIF, BigDecimal.valueOf(qty), motif, "retrouvaille");
    }

    private AjustementStockRequest negatifRequest(int qty, MotifAjustement motif) {
        return new AjustementStockRequest(stockId, TypeAjustement.NEGATIF, BigDecimal.valueOf(qty), motif, "perte rayon");
    }

    private MouvementStockResponse buildMouvementResponse() {
        return new MouvementStockResponse(
                UUID.randomUUID(), UUID.randomUUID(), null, null,
                new MouvementDetailResponse(MouvementStockType.AJUSTEMENT, new BigDecimal("20"), new BigDecimal("100"), new BigDecimal("120"), "RETROUVAILLE", null),
                null, null);
    }

    @Test
    void create_positif_should_create_lot_and_upsert_stock() {
        AjustementStockRequest req = positifRequest(20, MotifAjustement.RETROUVAILLE);
        Stock updated = new Stock();
        updated.setQuantiteDisponible(new BigDecimal("120"));

        when(stockService.findById(stockId)).thenReturn(stock);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(entreeStockService.createEntreeStock(any(EntreeStockCreate.class))).thenReturn(new EntreeStock());
        when(stockService.createOrUpdateEntry(any(StockEntryContext.class))).thenReturn(updated);
        when(mouvementStockService.journalize(eq(updated), any(MouvementJournalize.class))).thenReturn(buildMouvementResponse());

        MouvementStockResponse response = service.create(req);

        assertThat(response.detail().type()).isEqualTo(MouvementStockType.AJUSTEMENT);
        verify(entreeStockService).createEntreeStock(any(EntreeStockCreate.class));
    }

    @Test
    void create_negatif_should_consume_lots_fifo_without_sortie() {
        AjustementStockRequest req = negatifRequest(30, MotifAjustement.CASSE);
        EntreeStock l1 = new EntreeStock();
        l1.setId(UUID.randomUUID());
        l1.setQuantiteRestante(new BigDecimal("50"));
        Stock updated = new Stock();
        updated.setQuantiteDisponible(new BigDecimal("70"));

        when(stockService.findById(stockId)).thenReturn(stock);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(entreeStockService.findAvailableLotsForFifo(magasinId, productFournisseur.getId())).thenReturn(List.of(l1));
        when(stockService.decrement(stock, new BigDecimal("30"))).thenReturn(updated);
        when(mouvementStockService.journalize(eq(updated), any(MouvementJournalize.class))).thenReturn(null);

        service.create(req);

        assertThat(l1.getQuantiteRestante()).isEqualTo(new BigDecimal("20"));
        verify(stockService).decrement(stock, new BigDecimal("30"));
    }

    @Test
    void create_negatif_should_throw_when_insufficient_quantity() {
        AjustementStockRequest req = negatifRequest(30, MotifAjustement.PERTE);

        when(stockService.findById(stockId)).thenReturn(stock);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(entreeStockService.findAvailableLotsForFifo(magasinId, productFournisseur.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadArgumentException.class);

        verify(stockService, never()).decrement(any(), any(BigDecimal.class));
    }

    @Test
    void create_should_throw_when_motif_VOL_with_type_POSITIF() {
        AjustementStockRequest req = new AjustementStockRequest(stockId, TypeAjustement.POSITIF, new BigDecimal("20"), MotifAjustement.VOL, null);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadArgumentException.class);

        verify(stockService, never()).findById(any());
    }

    @Test
    void create_should_throw_when_motif_RETROUVAILLE_with_type_NEGATIF() {
        AjustementStockRequest req = new AjustementStockRequest(stockId, TypeAjustement.NEGATIF, new BigDecimal("20"), MotifAjustement.RETROUVAILLE, null);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void create_should_throw_when_motif_AUTRE_and_commentaire_blank() {
        AjustementStockRequest req = new AjustementStockRequest(stockId, TypeAjustement.POSITIF, new BigDecimal("5"), MotifAjustement.AUTRE, "   ");

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadArgumentException.class);

        verify(stockService, never()).findById(any());
    }

    @Test
    void create_should_throw_when_motif_AUTRE_and_commentaire_null() {
        AjustementStockRequest req = new AjustementStockRequest(stockId, TypeAjustement.POSITIF, new BigDecimal("5"), MotifAjustement.AUTRE, null);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadArgumentException.class);

        verify(stockService, never()).findById(any());
    }

    @Test
    void create_negatif_should_journalize_with_correct_quantities() {
        stock.setQuantiteDisponible(new BigDecimal("100"));
        AjustementStockRequest req = negatifRequest(30, MotifAjustement.CASSE);
        EntreeStock l1 = new EntreeStock();
        l1.setQuantiteRestante(new BigDecimal("50"));
        Stock updated = new Stock();
        updated.setQuantiteDisponible(new BigDecimal("70"));

        when(stockService.findById(stockId)).thenReturn(stock);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(entreeStockService.findAvailableLotsForFifo(magasinId, productFournisseur.getId())).thenReturn(List.of(l1));
        when(stockService.decrement(stock, new BigDecimal("30"))).thenReturn(updated);
        when(mouvementStockService.journalize(eq(updated), any(MouvementJournalize.class))).thenReturn(null);

        service.create(req);

        ArgumentCaptor<MouvementJournalize> captor = ArgumentCaptor.forClass(MouvementJournalize.class);
        verify(mouvementStockService).journalize(eq(updated), captor.capture());
        MouvementJournalize captured = captor.getValue();
        assertThat(captured.type()).isEqualTo(MouvementStockType.AJUSTEMENT);
        assertThat(captured.quantite()).isEqualTo(new BigDecimal("-30"));
        assertThat(captured.stockAvant()).isEqualTo(new BigDecimal("100"));
        assertThat(captured.stockApres()).isEqualTo(new BigDecimal("70"));
        assertThat(captured.referenceDocument()).isEqualTo("CASSE");
    }
}
