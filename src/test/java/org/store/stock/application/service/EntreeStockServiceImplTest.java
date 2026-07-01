package org.store.stock.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.application.service.IFournisseurService;
import org.store.achat.domain.model.Fournisseur;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.ForbiddenException;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.dto.ProductFournisseurResponse;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.produit.domain.model.Quality;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.EntreeStockCreate;
import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.application.dto.EntreeStockResponse;
import org.store.stock.application.dto.LigneEntreeStockRequest;
import org.store.stock.application.dto.MouvementJournalize;
import org.store.stock.application.dto.StockEntryContext;
import org.store.stock.application.service.impl.EntreeStockServiceImpl;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntreeStockServiceImplTest {

    @Mock private EntreeStockDomainService entreeStockDomainService;
    @Mock private StockDomainService stockDomainService;
    @Mock private MouvementStockDomainService mouvementStockDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private IFournisseurService fournisseurService;
    @Mock private IProductFournisseurService productFournisseurService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private IAuditEventPublisher auditEventPublisher;

    @InjectMocks
    private EntreeStockServiceImpl service;

    private UUID magasinId;
    private UUID fournisseurId;
    private UUID productFournisseurId;
    private UUID productId;
    private UUID qualityId;
    private UUID entrepriseId;
    private UUID accountId;

    private Magasin magasin;
    private Product produit;
    private Fournisseur fournisseur;
    private Quality quality;
    private ProductFournisseur productFournisseur;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
        fournisseurId = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();
        productId = UUID.randomUUID();
        qualityId = UUID.randomUUID();
        accountId = UUID.randomUUID();

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
        fournisseur.setId(fournisseurId);
        fournisseur.setNom("Fournisseur anonyme");
        fournisseur.setEntreprise(entreprise);

        quality = new Quality();
        quality.setId(qualityId);
        quality.setLibelle("Standard");
        quality.setEntreprise(entreprise);

        productFournisseur = new ProductFournisseur();
        productFournisseur.setId(productFournisseurId);
        productFournisseur.setProduct(produit);
        productFournisseur.setFournisseur(fournisseur);
        productFournisseur.setQuality(quality);
        productFournisseur.setPrixAchat(new BigDecimal("10.00"));
        productFournisseur.setPrixVente(new BigDecimal("15.00"));

        lenient().when(currentUserService.getCurrent()).thenReturn(new UserPrincipal(
                accountId, UUID.randomUUID(), entrepriseId, magasinId,
                "owner", null, null, "OWNER", List.of()));
    }

    private LigneEntreeStockRequest sampleLigne(int qty, BigDecimal prixAchat, BigDecimal prixVente, String numeroLot) {
        return new LigneEntreeStockRequest(productId, qualityId, qty, prixAchat, prixVente, numeroLot, LocalDate.now().plusYears(1));
    }

    private EntreeStockRequest sampleRequest(LigneEntreeStockRequest... lignes) {
        return new EntreeStockRequest(magasinId, fournisseurId, List.of(lignes));
    }

    private EntreeStock sampleSaved(int qty, BigDecimal prix, String numeroLot) {
        EntreeStock entreeStock = new EntreeStock();
        entreeStock.setId(UUID.randomUUID());
        entreeStock.setMagasin(magasin);
        entreeStock.setProduit(produit);
        entreeStock.setProductFournisseur(productFournisseur);
        entreeStock.setQuantiteInitiale(qty);
        entreeStock.setQuantiteRestante(qty);
        entreeStock.setPrixAchat(prix);
        entreeStock.setNumeroLot(numeroLot);
        entreeStock.setDateExpiration(LocalDate.now().plusYears(1));
        return entreeStock;
    }

    private void stubResolveMagasinAndFournisseur() {
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(fournisseurService.findById(fournisseurId)).thenReturn(fournisseur);
        when(fournisseurService.ensureBelongsToCurrentEntreprise(fournisseur)).thenReturn(fournisseur);
    }

    private void stubFindOrCreateProductFournisseur() {
        when(productFournisseurService.findOrCreate(any())).thenReturn(new ProductFournisseurResponse(productFournisseur));
        when(productFournisseurService.findById(productFournisseurId)).thenReturn(productFournisseur);
    }

    @Test
    void create_should_materialize_one_line_with_ENTREE_INITIAL_when_stock_absent() {
        EntreeStockRequest request = sampleRequest(sampleLigne(100, new BigDecimal("10.00"), new BigDecimal("15.00"), "LOT-001"));
        EntreeStock savedLot = sampleSaved(100, new BigDecimal("10.00"), "LOT-001");
        Stock upsertedStock = new Stock();
        upsertedStock.setId(UUID.randomUUID());
        upsertedStock.setMagasin(magasin);
        upsertedStock.setProductFournisseur(productFournisseur);
        upsertedStock.setQuantiteDisponible(100);
        upsertedStock.setPrixAchatMoyen(new BigDecimal("10.00"));

        stubResolveMagasinAndFournisseur();
        stubFindOrCreateProductFournisseur();
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, productFournisseurId)).thenReturn(Optional.empty());
        when(entreeStockDomainService.create(any(EntreeStockCreate.class))).thenReturn(savedLot);
        when(stockDomainService.createOrUpdateEntry(any(StockEntryContext.class))).thenReturn(upsertedStock);

        List<EntreeStockResponse> responses = service.create(request);

        assertThat(responses).hasSize(1);
        EntreeStockResponse response = responses.get(0);
        assertThat(response.id()).isEqualTo(savedLot.getId());
        assertThat(response.quantiteInitiale()).isEqualTo(100);
        assertThat(response.prixAchat()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(response.numeroLot()).isEqualTo("LOT-001");

        verify(mouvementStockDomainService).journalize(
                eq(upsertedStock),
                eq(new MouvementJournalize(MouvementStockType.ENTREE_INITIAL, 100, 0, 100, "LOT-001", null))
        );
        verify(productFournisseurService).applyPrixVenteFromPurchase(productFournisseur, new BigDecimal("15.00"));
    }

    @Test
    void create_should_materialize_multiple_lines_in_one_call() {
        EntreeStockRequest request = sampleRequest(
                sampleLigne(50, new BigDecimal("10.00"), new BigDecimal("15.00"), "LOT-A"),
                sampleLigne(30, new BigDecimal("10.00"), new BigDecimal("15.00"), "LOT-B")
        );

        stubResolveMagasinAndFournisseur();
        stubFindOrCreateProductFournisseur();
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, productFournisseurId)).thenReturn(Optional.empty());
        when(entreeStockDomainService.create(any(EntreeStockCreate.class)))
                .thenReturn(sampleSaved(50, new BigDecimal("10.00"), "LOT-A"))
                .thenReturn(sampleSaved(30, new BigDecimal("10.00"), "LOT-B"));
        Stock upserted = new Stock();
        upserted.setId(UUID.randomUUID());
        upserted.setMagasin(magasin);
        upserted.setProductFournisseur(productFournisseur);
        upserted.setQuantiteDisponible(50);
        when(stockDomainService.createOrUpdateEntry(any(StockEntryContext.class))).thenReturn(upserted);

        List<EntreeStockResponse> responses = service.create(request);

        assertThat(responses).hasSize(2);
        verify(entreeStockDomainService, times(2)).create(any(EntreeStockCreate.class));
        verify(stockDomainService, times(2)).createOrUpdateEntry(any(StockEntryContext.class));
        verify(mouvementStockDomainService, times(2)).journalize(any(), any());
        verify(productFournisseurService, times(2)).applyPrixVenteFromPurchase(any(), any());
    }

    @Test
    void create_should_publish_single_global_audit_event_for_the_batch() {
        EntreeStockRequest request = sampleRequest(
                sampleLigne(10, new BigDecimal("10.00"), new BigDecimal("15.00"), "L1"),
                sampleLigne(20, new BigDecimal("10.00"), new BigDecimal("15.00"), "L2"),
                sampleLigne(30, new BigDecimal("10.00"), new BigDecimal("15.00"), "L3")
        );

        stubResolveMagasinAndFournisseur();
        stubFindOrCreateProductFournisseur();
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, productFournisseurId)).thenReturn(Optional.empty());
        when(entreeStockDomainService.create(any(EntreeStockCreate.class))).thenReturn(sampleSaved(10, new BigDecimal("10.00"), "L1"));
        Stock upserted = new Stock();
        upserted.setId(UUID.randomUUID());
        upserted.setQuantiteDisponible(10);
        when(stockDomainService.createOrUpdateEntry(any(StockEntryContext.class))).thenReturn(upserted);

        service.create(request);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher, times(1)).publish(captor.capture());
        AuditEvent event = captor.getValue();
        assertThat(event.action()).isEqualTo(AuditAction.STOCK_INITIAL_ENTRY);
        assertThat(event.entityType()).isEqualTo(AuditEntityType.STOCK);
        assertThat(event.entityId()).isNull();
        assertThat(event.entityLabel()).isEqualTo("ENTREE INITIAL");
        assertThat(event.magasinId()).isEqualTo(magasinId);
        assertThat(event.entrepriseId()).isEqualTo(entrepriseId);
        assertThat(event.performedBy()).isEqualTo(accountId.toString());
    }

    @Test
    void create_should_use_existing_stockAvant_when_stock_already_exists() {
        EntreeStockRequest request = sampleRequest(sampleLigne(50, new BigDecimal("20.00"), new BigDecimal("30.00"), "LOT-X"));
        Stock existingStock = new Stock();
        existingStock.setId(UUID.randomUUID());
        existingStock.setMagasin(magasin);
        existingStock.setProductFournisseur(productFournisseur);
        existingStock.setQuantiteDisponible(100);
        existingStock.setPrixAchatMoyen(new BigDecimal("10.00"));
        Stock upsertedStock = new Stock();
        upsertedStock.setId(existingStock.getId());
        upsertedStock.setQuantiteDisponible(150);
        upsertedStock.setPrixAchatMoyen(new BigDecimal("13.33"));

        stubResolveMagasinAndFournisseur();
        stubFindOrCreateProductFournisseur();
        when(stockDomainService.findByMagasinIdAndProductFournisseurId(magasinId, productFournisseurId)).thenReturn(Optional.of(existingStock));
        when(entreeStockDomainService.create(any(EntreeStockCreate.class))).thenReturn(sampleSaved(50, new BigDecimal("20.00"), "LOT-X"));
        when(stockDomainService.createOrUpdateEntry(any(StockEntryContext.class))).thenReturn(upsertedStock);

        service.create(request);

        ArgumentCaptor<MouvementJournalize> captor = ArgumentCaptor.forClass(MouvementJournalize.class);
        verify(mouvementStockDomainService).journalize(eq(upsertedStock), captor.capture());
        MouvementJournalize captured = captor.getValue();
        assertThat(captured.type()).isEqualTo(MouvementStockType.ENTREE_INITIAL);
        assertThat(captured.quantite()).isEqualTo(50);
        assertThat(captured.stockAvant()).isEqualTo(100);
        assertThat(captured.stockApres()).isEqualTo(150);
    }

    @Test
    void create_should_propagate_when_prixVente_not_greater_than_prixAchat() {
        EntreeStockRequest request = sampleRequest(sampleLigne(10, new BigDecimal("15.00"), new BigDecimal("15.00"), "LOT-X"));

        stubResolveMagasinAndFournisseur();
        doThrow(new BadArgumentException("productFournisseur.prixVente.belowOrEqualAchat"))
                .when(productFournisseurService).ensurePrixVenteGreaterThanPrixAchat(new BigDecimal("15.00"), new BigDecimal("15.00"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadArgumentException.class);

        verify(entreeStockDomainService, never()).create(any(EntreeStockCreate.class));
        verify(mouvementStockDomainService, never()).journalize(any(), any());
        verify(auditEventPublisher, never()).publish(any());
    }

    @Test
    void create_should_propagate_forbidden_when_magasin_not_accessible() {
        EntreeStockRequest request = sampleRequest(sampleLigne(10, new BigDecimal("10.00"), new BigDecimal("15.00"), "LOT-X"));

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ForbiddenException.class);

        verify(entreeStockDomainService, never()).create(any(EntreeStockCreate.class));
        verify(auditEventPublisher, never()).publish(any());
    }

    @Test
    void create_should_propagate_forbidden_when_fournisseur_not_in_entreprise() {
        EntreeStockRequest request = sampleRequest(sampleLigne(10, new BigDecimal("10.00"), new BigDecimal("15.00"), "LOT-X"));

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(fournisseurService.findById(fournisseurId)).thenReturn(fournisseur);
        when(fournisseurService.ensureBelongsToCurrentEntreprise(fournisseur))
                .thenThrow(new ForbiddenException("fournisseur.notOwned"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ForbiddenException.class);

        verify(entreeStockDomainService, never()).create(any(EntreeStockCreate.class));
        verify(auditEventPublisher, never()).publish(any());
    }
}
