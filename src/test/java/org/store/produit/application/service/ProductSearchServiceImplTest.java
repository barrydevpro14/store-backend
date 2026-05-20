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
import org.store.achat.domain.model.Fournisseur;
import org.store.common.exceptions.BadArgumentException;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.application.dto.ProductSearchResponse;
import org.store.produit.application.service.impl.ProductSearchServiceImpl;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.produit.domain.model.Quality;
import org.store.produit.domain.service.ProductDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.service.IEntreeStockService;
import org.store.stock.domain.model.EntreeStock;

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
    @Mock private IEntreeStockService entreeStockService;
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
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, null, "owner", "OWNER",
                List.of("PRODUCT_READ"));
    }

    private UserPrincipal vendeur(UUID magasinScopedId) {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, magasinScopedId, "vendeur", "SELLER",
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

    @Test
    void search_should_throw_when_proprietaire_and_magasinId_absent() {
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThatThrownBy(() -> service.search("clou", null, PageRequest.of(0, 10)))
                .isInstanceOf(BadArgumentException.class);

        verify(productDomainService, never()).searchByEntrepriseWithActiveLots(any(), any(), any(), any());
    }

    @Test
    void search_should_use_employee_magasin_when_param_absent() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = sampleProduct();

        when(currentUserService.getCurrent()).thenReturn(vendeur(magasinId));
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productDomainService.searchByEntrepriseWithActiveLots("clou", magasinId, entrepriseId, pageable))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));
        when(entreeStockService.findActiveLotsByMagasinAndProductIds(magasinId, List.of(productId)))
                .thenReturn(List.of(buildLot(product, BigDecimal.valueOf(15), 8)));

        Page<ProductSearchResponse> result = service.search("clou", null, pageable);

        assertThat(result.getContent()).hasSize(1);
        ProductSearchResponse productResponse = result.getContent().get(0);
        assertThat(productResponse.quantiteEnStock()).isEqualTo(8);
        assertThat(productResponse.productFournisseurs()).hasSize(1);
        assertThat(productResponse.productFournisseurs().get(0).prixVente()).isEqualByComparingTo("15");
    }

    @Test
    void search_should_aggregate_lots_by_product_fournisseur() {
        Pageable pageable = PageRequest.of(0, 10);
        Product product = sampleProduct();

        EntreeStock lotChineFirst = buildLot(product, BigDecimal.valueOf(10), 5);
        EntreeStock lotChineSecond = buildLotForPf(product, lotChineFirst.getProductFournisseur(), 7);
        EntreeStock lotMaroc = buildLot(product, BigDecimal.valueOf(15), 3);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productDomainService.searchByEntrepriseWithActiveLots("clou", magasinId, entrepriseId, pageable))
                .thenReturn(new PageImpl<>(List.of(product), pageable, 1));
        when(entreeStockService.findActiveLotsByMagasinAndProductIds(magasinId, List.of(productId)))
                .thenReturn(List.of(lotChineFirst, lotChineSecond, lotMaroc));

        Page<ProductSearchResponse> result = service.search("clou", magasinId, pageable);

        ProductSearchResponse productResponse = result.getContent().get(0);
        assertThat(productResponse.quantiteEnStock()).isEqualTo(15);
        assertThat(productResponse.productFournisseurs()).hasSize(2);
    }

    @Test
    void search_should_return_empty_page_when_no_match() {
        Pageable pageable = PageRequest.of(0, 10);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(productDomainService.searchByEntrepriseWithActiveLots("absent", magasinId, entrepriseId, pageable))
                .thenReturn(Page.empty(pageable));

        Page<ProductSearchResponse> result = service.search("absent", magasinId, pageable);

        assertThat(result.getContent()).isEmpty();
        verify(entreeStockService, never()).findActiveLotsByMagasinAndProductIds(any(), any());
    }

    private EntreeStock buildLot(Product product, BigDecimal prixVente, int quantiteRestante) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(UUID.randomUUID());
        fournisseur.setNom("Fournisseur " + UUID.randomUUID());

        Quality quality = new Quality();
        quality.setId(UUID.randomUUID());
        quality.setLibelle("Original");

        ProductFournisseur productFournisseur = new ProductFournisseur();
        productFournisseur.setId(UUID.randomUUID());
        productFournisseur.setProduct(product);
        productFournisseur.setFournisseur(fournisseur);
        productFournisseur.setQuality(quality);
        productFournisseur.setPrixAchat(prixVente.subtract(BigDecimal.ONE));
        productFournisseur.setPrixVente(prixVente);

        return buildLotForPf(product, productFournisseur, quantiteRestante);
    }

    private EntreeStock buildLotForPf(Product product, ProductFournisseur productFournisseur, int quantiteRestante) {
        EntreeStock lot = new EntreeStock();
        lot.setId(UUID.randomUUID());
        lot.setProduit(product);
        lot.setProductFournisseur(productFournisseur);
        lot.setQuantiteRestante(quantiteRestante);
        return lot;
    }
}
