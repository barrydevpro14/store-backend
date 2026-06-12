package org.store.stock.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.notification.application.service.INotificationEventPublisher;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.SortieStockCreate;
import org.store.stock.application.dto.SortieStockForVente;
import org.store.stock.application.dto.SortieStockRequest;
import org.store.stock.application.dto.SortieStockResponse;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.model.LigneCommandeVente;
import org.store.stock.application.service.impl.SortieStockServiceImpl;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.model.EntreeStock;
import org.store.stock.domain.model.SortieStock;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.EntreeStockDomainService;
import org.store.stock.domain.service.MouvementStockDomainService;
import org.store.stock.domain.service.SortieStockDomainService;
import org.store.stock.domain.service.StockDomainService;

import java.math.BigDecimal;
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
class SortieStockServiceImplTest {

    @Mock private EntreeStockDomainService entreeStockDomainService;
    @Mock private SortieStockDomainService sortieStockDomainService;
    @Mock private StockDomainService stockDomainService;
    @Mock private MouvementStockDomainService mouvementStockDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private IProductFournisseurService productFournisseurService;
    @Mock private INotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private SortieStockServiceImpl service;

    private UUID magasinId;
    private UUID productId;
    private UUID productFournisseurId;
    private Entreprise entreprise;
    private Magasin magasin;
    private Product produit;
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

        productFournisseur = new ProductFournisseur();
        productFournisseur.setId(productFournisseurId);
        productFournisseur.setProduct(produit);

        stock = new Stock();
        stock.setId(UUID.randomUUID());
        stock.setMagasin(magasin);
        stock.setProductFournisseur(productFournisseur);
        stock.setQuantiteDisponible(300);
    }

    private SortieStockRequest request(int quantite, BigDecimal prixVente) {
        return new SortieStockRequest(magasinId, productFournisseurId, quantite, prixVente, "vente comptoir");
    }

    private EntreeStock lot(int qtyRestante, BigDecimal prixAchat) {
        EntreeStock lot = new EntreeStock();
        lot.setId(UUID.randomUUID());
        lot.setMagasin(magasin);
        lot.setProduit(produit);
        lot.setQuantiteInitiale(qtyRestante);
        lot.setQuantiteRestante(qtyRestante);
        lot.setPrixAchat(prixAchat);
        return lot;
    }

    private SortieStock buildSortie(EntreeStock lot, int qty, BigDecimal prixVente) {
        SortieStock sortie = new SortieStock();
        sortie.setId(UUID.randomUUID());
        sortie.setEntreeStock(lot);
        sortie.setQuantiteSortie(qty);
        sortie.setPrixAchat(lot.getPrixAchat());
        sortie.setPrixVente(prixVente);
        sortie.setMarge(prixVente.subtract(lot.getPrixAchat()).multiply(BigDecimal.valueOf(qty)));
        return sortie;
    }

    @Test
    void create_should_consume_single_lot_when_quantite_fits() {
        EntreeStock l1 = lot(100, new BigDecimal("10.00"));
        SortieStockRequest req = request(50, new BigDecimal("30.00"));

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, productFournisseurId)).thenReturn(Optional.of(stock));
        when(entreeStockDomainService.findAvailableLotsForFifoByProductFournisseur(magasinId, productFournisseurId)).thenReturn(List.of(l1));
        when(entreeStockDomainService.save(any(EntreeStock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sortieStockDomainService.create(eq(new SortieStockCreate(l1, 50, new BigDecimal("30.00"), null))))
                .thenReturn(buildSortie(l1, 50, new BigDecimal("30.00")));
        when(stockDomainService.decrement(stock, 50)).thenAnswer(inv -> {
            stock.setQuantiteDisponible(250);
            return stock;
        });

        List<SortieStockResponse> result = service.create(req);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().quantiteSortie()).isEqualTo(50);
        assertThat(l1.getQuantiteRestante()).isEqualTo(50);
    }

    @Test
    void create_should_consume_multiple_lots_fifo() {
        EntreeStock l1 = lot(100, new BigDecimal("10.00"));
        EntreeStock l2 = lot(100, new BigDecimal("15.00"));
        EntreeStock l3 = lot(100, new BigDecimal("25.00"));
        SortieStockRequest req = request(150, new BigDecimal("30.00"));

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, productFournisseurId)).thenReturn(Optional.of(stock));
        when(entreeStockDomainService.findAvailableLotsForFifoByProductFournisseur(magasinId, productFournisseurId)).thenReturn(List.of(l1, l2, l3));
        when(entreeStockDomainService.save(any(EntreeStock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sortieStockDomainService.create(eq(new SortieStockCreate(l1, 100, new BigDecimal("30.00"), null))))
                .thenReturn(buildSortie(l1, 100, new BigDecimal("30.00")));
        when(sortieStockDomainService.create(eq(new SortieStockCreate(l2, 50, new BigDecimal("30.00"), null))))
                .thenReturn(buildSortie(l2, 50, new BigDecimal("30.00")));
        when(stockDomainService.decrement(stock, 150)).thenAnswer(inv -> {
            stock.setQuantiteDisponible(150);
            return stock;
        });

        List<SortieStockResponse> result = service.create(req);

        assertThat(result).hasSize(2);
        assertThat(l1.getQuantiteRestante()).isEqualTo(0);
        assertThat(l2.getQuantiteRestante()).isEqualTo(50);
        assertThat(l3.getQuantiteRestante()).isEqualTo(100);

        assertThat(result.get(0).marge()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(result.get(1).marge()).isEqualByComparingTo(new BigDecimal("750.00"));
    }

    @Test
    void create_should_throw_when_stock_not_found() {
        SortieStockRequest req = request(10, new BigDecimal("30.00"));

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, productFournisseurId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(EntityException.class);

        verify(sortieStockDomainService, never()).create(any(), anyInt(), any());
        verify(mouvementStockDomainService, never()).journalize(any(), any());
    }

    @Test
    void create_should_throw_when_insufficient_quantity() {
        stock.setQuantiteDisponible(50);
        SortieStockRequest req = request(100, new BigDecimal("30.00"));

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, productFournisseurId)).thenReturn(Optional.of(stock));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadArgumentException.class);

        verify(sortieStockDomainService, never()).create(any(), anyInt(), any());
        verify(mouvementStockDomainService, never()).journalize(any(), any());
    }

    @Test
    void create_should_journalize_movement_with_correct_quantities() {
        EntreeStock l1 = lot(100, new BigDecimal("10.00"));
        SortieStockRequest req = request(50, new BigDecimal("30.00"));

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
        when(productFournisseurService.ensureBelongsToCurrentEntreprise(productFournisseur)).thenReturn(productFournisseur);
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, productFournisseurId)).thenReturn(Optional.of(stock));
        when(entreeStockDomainService.findAvailableLotsForFifoByProductFournisseur(magasinId, productFournisseurId)).thenReturn(List.of(l1));
        when(entreeStockDomainService.save(any(EntreeStock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sortieStockDomainService.create(eq(new SortieStockCreate(l1, 50, new BigDecimal("30.00"), null))))
                .thenReturn(buildSortie(l1, 50, new BigDecimal("30.00")));
        when(stockDomainService.decrement(stock, 50)).thenAnswer(inv -> {
            stock.setQuantiteDisponible(250);
            return stock;
        });

        service.create(req);

        ArgumentCaptor<MouvementJournalize> captor = ArgumentCaptor.forClass(MouvementJournalize.class);
        verify(mouvementStockDomainService).journalize(eq(stock), captor.capture());
        MouvementJournalize captured = captor.getValue();
        assertThat(captured.type()).isEqualTo(MouvementStockType.SORTIE_VENTE);
        assertThat(captured.quantite()).isEqualTo(50);
        assertThat(captured.stockAvant()).isEqualTo(300);
        assertThat(captured.stockApres()).isEqualTo(250);
        assertThat(captured.commentaire()).isEqualTo("vente comptoir");
    }

    @Test
    void create_should_propagate_forbidden_when_magasin_not_accessible() {
        SortieStockRequest req = request(10, new BigDecimal("30.00"));

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void consumeForVente_should_consume_single_lot_and_link_ligneVente() {
        ProductFournisseur pf = pf();
        LigneCommandeVente ligne = ligneVente();
        EntreeStock l1 = lot(100, new BigDecimal("10.00"));
        SortieStockForVente sortieForVente = new SortieStockForVente(magasin, pf, 30, new BigDecimal("25.00"), ligne);

        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, pf.getId())).thenReturn(Optional.of(stock));
        when(entreeStockDomainService.findAvailableLotsForFifoByProductFournisseur(magasinId, pf.getId())).thenReturn(List.of(l1));
        when(entreeStockDomainService.save(any(EntreeStock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sortieStockDomainService.create(eq(new SortieStockCreate(l1, 30, new BigDecimal("25.00"), ligne))))
                .thenReturn(buildSortie(l1, 30, new BigDecimal("25.00")));
        when(stockDomainService.decrement(stock, 30)).thenAnswer(inv -> {
            stock.setQuantiteDisponible(270);
            return stock;
        });

        List<SortieStockResponse> result = service.consumeForVente(sortieForVente);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().quantiteSortie()).isEqualTo(30);
        assertThat(l1.getQuantiteRestante()).isEqualTo(70);
    }

    @Test
    void consumeForVente_should_consume_multiple_lots_fifo_scoped_by_pf() {
        ProductFournisseur pf = pf();
        LigneCommandeVente ligne = ligneVente();
        EntreeStock l1 = lot(50, new BigDecimal("10.00"));
        EntreeStock l2 = lot(50, new BigDecimal("12.00"));
        SortieStockForVente sortieForVente = new SortieStockForVente(magasin, pf, 80, new BigDecimal("25.00"), ligne);

        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, pf.getId())).thenReturn(Optional.of(stock));
        when(entreeStockDomainService.findAvailableLotsForFifoByProductFournisseur(magasinId, pf.getId())).thenReturn(List.of(l1, l2));
        when(entreeStockDomainService.save(any(EntreeStock.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sortieStockDomainService.create(eq(new SortieStockCreate(l1, 50, new BigDecimal("25.00"), ligne))))
                .thenReturn(buildSortie(l1, 50, new BigDecimal("25.00")));
        when(sortieStockDomainService.create(eq(new SortieStockCreate(l2, 30, new BigDecimal("25.00"), ligne))))
                .thenReturn(buildSortie(l2, 30, new BigDecimal("25.00")));
        when(stockDomainService.decrement(stock, 80)).thenAnswer(inv -> {
            stock.setQuantiteDisponible(220);
            return stock;
        });

        List<SortieStockResponse> result = service.consumeForVente(sortieForVente);

        assertThat(result).hasSize(2);
        assertThat(l1.getQuantiteRestante()).isEqualTo(0);
        assertThat(l2.getQuantiteRestante()).isEqualTo(20);
    }

    @Test
    void consumeForVente_should_throw_when_pf_lots_insufficient() {
        ProductFournisseur pf = pf();
        LigneCommandeVente ligne = ligneVente();
        EntreeStock l1 = lot(20, new BigDecimal("10.00"));
        SortieStockForVente sortieForVente = new SortieStockForVente(magasin, pf, 50, new BigDecimal("25.00"), ligne);

        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, pf.getId())).thenReturn(Optional.of(stock));
        when(entreeStockDomainService.findAvailableLotsForFifoByProductFournisseur(magasinId, pf.getId())).thenReturn(List.of(l1));

        assertThatThrownBy(() -> service.consumeForVente(sortieForVente))
                .isInstanceOf(BadArgumentException.class);

        verify(sortieStockDomainService, never()).create(any(SortieStockCreate.class));
        verify(mouvementStockDomainService, never()).journalize(any(), any());
    }

    private ProductFournisseur pf() {
        ProductFournisseur pf = new ProductFournisseur();
        pf.setId(UUID.randomUUID());
        pf.setProduct(produit);
        return pf;
    }

    private LigneCommandeVente ligneVente() {
        CommandeVente commande = new CommandeVente();
        commande.setReference("VNT-TEST");

        LigneCommandeVente ligne = new LigneCommandeVente();
        ligne.setId(UUID.randomUUID());
        ligne.setCommande(commande);
        return ligne;
    }

    private static int anyInt() {
        return org.mockito.ArgumentMatchers.anyInt();
    }
}
