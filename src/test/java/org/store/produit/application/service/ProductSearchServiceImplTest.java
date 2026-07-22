package org.store.produit.application.service;

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
import org.store.common.exceptions.BadArgumentException;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.dto.ProductSelectorResponse;
import org.store.produit.application.dto.ProductVariantSearchResponse;
import org.store.produit.application.service.impl.ProductSearchServiceImpl;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.service.ProductDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceImplTest {

    @Mock private ProductDomainService productDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private ICurrentUserService currentUserService;

    @InjectMocks
    private ProductSearchServiceImpl service;

    private UUID entrepriseId;
    private UUID productId;
    private UUID magasinId;
    private Entreprise entreprise;
    private Magasin magasin;
    private CategoryProduct category;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        productId = UUID.randomUUID();
        magasinId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);

        category = new CategoryProduct();
        category.setId(UUID.randomUUID());
        category.setLibelle("Visserie");
        category.setEntreprise(entreprise);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, null, "owner", null, null, "OWNER",
                List.of("PRODUCT_READ"));
    }

    private UserPrincipal vendeur(UUID magasinScopedId) {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, magasinScopedId, "vendeur", null, null, "SELLER",
                List.of("PRODUCT_READ"));
    }

    private Product sampleProduct() {
        Product product = new Product();
        product.setId(productId);
        product.setNom("Clou 10mm");
        product.setReference("CL-10");
        product.setCategoryProduct(category);
        product.setEntreprise(entreprise);
        return product;
    }

    private ProductVariantSearchResponse sampleVariant() {
        return new ProductVariantSearchResponse(
                UUID.randomUUID(), productId, UUID.randomUUID(), UUID.randomUUID(),
                "Clou 10mm (CL-10) — Visserie — DistribA — Neuf (5)",
                new BigDecimal("8.00"), new BigDecimal("12.00"));
    }

    // ── search ──────────────────────────────────────────────────────────────

    @Test
    void search_should_throw_when_proprietaire_and_magasinId_absent() {
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThatThrownBy(() -> service.search("clou", null, PageRequest.of(0, 10)))
                .isInstanceOf(BadArgumentException.class);

        verify(productDomainService, never()).searchVariants(any(), any(), any(), any());
    }

    @Test
    void search_should_use_employee_magasin_when_param_absent() {
        Pageable pageable = PageRequest.of(0, 10);
        ProductVariantSearchResponse variant = sampleVariant();

        when(currentUserService.getCurrent()).thenReturn(vendeur(magasinId));
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productDomainService.searchVariants("clou", magasinId, entrepriseId, pageable))
                .thenReturn(new PageImpl<>(List.of(variant), pageable, 1));

        Page<ProductVariantSearchResponse> result = service.search("clou", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).productId()).isEqualTo(productId);
        assertThat(result.getContent().get(0).label()).endsWith("(5)");
    }

    @Test
    void search_should_return_empty_page_when_no_match() {
        Pageable pageable = PageRequest.of(0, 10);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productDomainService.searchVariants("absent", magasinId, entrepriseId, pageable))
                .thenReturn(Page.empty(pageable));

        Page<ProductVariantSearchResponse> result = service.search("absent", magasinId, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    // ── searchAll ────────────────────────────────────────────────────────────

    @Test
    void searchAll_should_throw_when_proprietaire_and_magasinId_absent() {
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThatThrownBy(() -> service.searchAll("clou", null, PageRequest.of(0, 10)))
                .isInstanceOf(BadArgumentException.class);

        verify(productDomainService, never()).searchResponsesByEntreprise(any(), any(), any());
    }

    @Test
    void searchAll_should_return_products_without_stock_check() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = sampleProduct();
        ProductSelectorResponse expected = new ProductSelectorResponse(product);

        when(currentUserService.getCurrent()).thenReturn(vendeur(magasinId));
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productDomainService.searchResponsesByEntreprise("clou", entrepriseId, pageable))
                .thenReturn(new PageImpl<>(List.of(expected), pageable, 1));

        Page<ProductSelectorResponse> result = service.searchAll("clou", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(productId);
        assertThat(result.getContent().get(0).nom()).isEqualTo(product.getNom());
        assertThat(result.getContent().get(0).reference()).isEqualTo(product.getReference());

        verify(productDomainService, never()).searchVariants(any(), any(), any(), any());
    }

    @Test
    void searchAll_should_return_empty_page_when_no_match() {
        Pageable pageable = PageRequest.of(0, 10);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productDomainService.searchResponsesByEntreprise("absent", entrepriseId, pageable))
                .thenReturn(Page.empty(pageable));

        Page<ProductSelectorResponse> result = service.searchAll("absent", magasinId, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
