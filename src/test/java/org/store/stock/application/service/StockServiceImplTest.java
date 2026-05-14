package org.store.stock.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.exceptions.ForbiddenException;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.produit.application.service.IProductService;
import org.store.produit.domain.model.Product;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.application.service.impl.StockServiceImpl;
import org.store.stock.domain.model.Stock;
import org.store.stock.domain.service.StockDomainService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock private StockDomainService stockDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private IProductService productService;
    @Mock private ICurrentUserService currentUserService;

    @InjectMocks
    private StockServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private UUID productId;
    private UUID stockId;

    private Entreprise entreprise;
    private Magasin magasin;
    private Product produit;
    private Stock stock;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
        productId = UUID.randomUUID();
        stockId = UUID.randomUUID();

        entreprise = new Entreprise();
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

        stock = new Stock();
        stock.setId(stockId);
        stock.setMagasin(magasin);
        stock.setProduit(produit);
        stock.setQuantiteDisponible(150);
        stock.setSeuilApprovisionnement(20);
        stock.setPrixAchatMoyen(new BigDecimal("13.33"));
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), entrepriseId, null, "owner", "PROPRIETAIRE", List.of("STOCK_READ"));
    }

    private UserPrincipal employe(UUID employeMagasinId) {
        return new UserPrincipal(UUID.randomUUID(), entrepriseId, employeMagasinId, "vendor", "VENDEUR", List.of("STOCK_READ"));
    }

    @Test
    void findResponseById_should_return_when_accessible() {
        when(stockDomainService.findById(stockId)).thenReturn(stock);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);

        StockResponse response = service.findResponseById(stockId);

        assertThat(response.id()).isEqualTo(stockId);
        assertThat(response.magasin().id()).isEqualTo(magasinId);
        assertThat(response.produit().id()).isEqualTo(productId);
        assertThat(response.quantiteDisponible()).isEqualTo(150);
        assertThat(response.prixAchatMoyen()).isEqualByComparingTo(new BigDecimal("13.33"));
    }

    @Test
    void findResponseById_should_propagate_forbidden_when_magasin_not_accessible() {
        when(stockDomainService.findById(stockId)).thenReturn(stock);
        when(magasinService.ensureAccessibleByCurrentUser(magasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.findResponseById(stockId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void list_for_proprietaire_without_filter_returns_all_entreprise() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StockResponse> page = new PageImpl<>(List.of(sampleResponse()), pageable, 1);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(stockDomainService.findResponsesByFilters(eq(entrepriseId), isNull(), isNull(), eq(pageable))).thenReturn(page);

        Page<StockResponse> result = service.findAllByCurrentEntreprise(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void list_for_employe_without_filter_forces_own_magasin() {
        Pageable pageable = PageRequest.of(0, 10);
        UUID employeMagasinId = UUID.randomUUID();
        Page<StockResponse> page = new PageImpl<>(List.of(), pageable, 0);

        when(currentUserService.getCurrent()).thenReturn(employe(employeMagasinId));
        when(stockDomainService.findResponsesByFilters(eq(entrepriseId), eq(employeMagasinId), isNull(), eq(pageable))).thenReturn(page);

        service.findAllByCurrentEntreprise(null, null, pageable);

        verify(stockDomainService).findResponsesByFilters(eq(entrepriseId), eq(employeMagasinId), isNull(), eq(pageable));
    }

    @Test
    void list_with_magasin_filter_checks_access() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StockResponse> page = new PageImpl<>(List.of(), pageable, 0);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(stockDomainService.findResponsesByFilters(eq(entrepriseId), eq(magasinId), isNull(), eq(pageable))).thenReturn(page);

        service.findAllByCurrentEntreprise(magasinId, null, pageable);

        verify(magasinService).ensureAccessibleByCurrentUser(magasin);
    }

    @Test
    void list_with_magasin_filter_propagates_forbidden_when_not_accessible() {
        Pageable pageable = PageRequest.of(0, 10);

        when(currentUserService.getCurrent()).thenReturn(employe(UUID.randomUUID()));
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.findAllByCurrentEntreprise(magasinId, null, pageable))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void list_with_product_filter_checks_ownership() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StockResponse> page = new PageImpl<>(List.of(), pageable, 0);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productService.findById(productId)).thenReturn(produit);
        when(productService.ensureBelongsToCurrentEntreprise(produit)).thenReturn(produit);
        when(stockDomainService.findResponsesByFilters(eq(entrepriseId), isNull(), eq(productId), eq(pageable))).thenReturn(page);

        service.findAllByCurrentEntreprise(null, productId, pageable);

        verify(productService).ensureBelongsToCurrentEntreprise(produit);
    }

    private StockResponse sampleResponse() {
        return new StockResponse(
                stockId,
                new MagasinSummaryResponse(magasinId, "Magasin Central"),
                new ProductSummaryResponse(productId, "Clou 10mm", "CL-10"),
                150, 20, new BigDecimal("13.33"),
                null, null
        );
    }
}
