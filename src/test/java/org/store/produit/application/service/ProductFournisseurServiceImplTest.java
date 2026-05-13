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
import org.store.achat.application.dto.FournisseurSummaryResponse;
import org.store.achat.application.service.IFournisseurService;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.entreprise.domain.model.Entreprise;
import org.store.produit.application.dto.ProductFournisseurRequest;
import org.store.produit.application.dto.ProductFournisseurResponse;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.produit.application.service.impl.ProductFournisseurServiceImpl;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.produit.domain.service.ProductFournisseurDomainService;
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
class ProductFournisseurServiceImplTest {

    @Mock private ProductFournisseurDomainService productFournisseurDomainService;
    @Mock private IProductService productService;
    @Mock private IFournisseurService fournisseurService;
    @Mock private ICurrentUserService currentUserService;

    @InjectMocks
    private ProductFournisseurServiceImpl service;

    private UUID entrepriseId;
    private UUID productFournisseurId;
    private UUID productId;
    private UUID fournisseurId;
    private Entreprise entreprise;
    private Product product;
    private Fournisseur fournisseur;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();
        productId = UUID.randomUUID();
        fournisseurId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        product = new Product();
        product.setId(productId);
        product.setNom("Pneu 195/65 R15");
        product.setReference("PN-195");
        product.setEntreprise(entreprise);

        fournisseur = new Fournisseur();
        fournisseur.setId(fournisseurId);
        fournisseur.setNom("Pneus Maroc SARL");
        fournisseur.setEntreprise(entreprise);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), entrepriseId, UUID.randomUUID(), "owner", "PROPRIETAIRE",
                List.of("SUPPLIER_CREATE", "SUPPLIER_READ"));
    }

    private ProductFournisseur sample() {
        ProductFournisseur pf = new ProductFournisseur();
        pf.setId(productFournisseurId);
        pf.setProduct(product);
        pf.setFournisseur(fournisseur);
        pf.setPrixAchat(new BigDecimal("12.50"));
        pf.setReferenceFournisseur("REF-FRN-001");
        pf.setOrigine("Maroc");
        return pf;
    }

    @Test
    void create_should_persist_when_inputs_valid() {
        ProductFournisseurRequest request = new ProductFournisseurRequest(
                productId, fournisseurId, new BigDecimal("12.50"), "REF-FRN-001", "Maroc"
        );
        ProductFournisseur saved = sample();

        when(productService.findById(productId)).thenReturn(product);
        when(productService.ensureBelongsToCurrentEntreprise(product)).thenReturn(product);
        when(fournisseurService.findById(fournisseurId)).thenReturn(fournisseur);
        when(fournisseurService.ensureBelongsToCurrentEntreprise(fournisseur)).thenReturn(fournisseur);
        when(productFournisseurDomainService.existsByProductIdAndFournisseurId(productId, fournisseurId)).thenReturn(false);
        when(productFournisseurDomainService.create(request, product, fournisseur)).thenReturn(saved);

        ProductFournisseurResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(productFournisseurId);
        assertThat(response.product().id()).isEqualTo(productId);
        assertThat(response.fournisseur().id()).isEqualTo(fournisseurId);
        assertThat(response.prixAchat()).isEqualByComparingTo(new BigDecimal("12.50"));
        assertThat(response.referenceFournisseur()).isEqualTo("REF-FRN-001");
        assertThat(response.origine()).isEqualTo("Maroc");
    }

    @Test
    void create_should_throw_when_pair_already_exists() {
        ProductFournisseurRequest request = new ProductFournisseurRequest(
                productId, fournisseurId, new BigDecimal("12.50"), null, null
        );

        when(productService.findById(productId)).thenReturn(product);
        when(productService.ensureBelongsToCurrentEntreprise(product)).thenReturn(product);
        when(fournisseurService.findById(fournisseurId)).thenReturn(fournisseur);
        when(fournisseurService.ensureBelongsToCurrentEntreprise(fournisseur)).thenReturn(fournisseur);
        when(productFournisseurDomainService.existsByProductIdAndFournisseurId(productId, fournisseurId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UniqueResourceException.class);

        verify(productFournisseurDomainService, never()).create(any(), any(), any());
    }

    @Test
    void create_should_throw_when_product_belongs_to_other_entreprise() {
        ProductFournisseurRequest request = new ProductFournisseurRequest(
                productId, fournisseurId, new BigDecimal("12.50"), null, null
        );

        when(productService.findById(productId)).thenReturn(product);
        when(productService.ensureBelongsToCurrentEntreprise(product))
                .thenThrow(new ForbiddenException("product.notOwned"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ForbiddenException.class);

        verify(productFournisseurDomainService, never()).create(any(), any(), any());
    }

    @Test
    void create_should_throw_when_fournisseur_belongs_to_other_entreprise() {
        ProductFournisseurRequest request = new ProductFournisseurRequest(
                productId, fournisseurId, new BigDecimal("12.50"), null, null
        );

        when(productService.findById(productId)).thenReturn(product);
        when(productService.ensureBelongsToCurrentEntreprise(product)).thenReturn(product);
        when(fournisseurService.findById(fournisseurId)).thenReturn(fournisseur);
        when(fournisseurService.ensureBelongsToCurrentEntreprise(fournisseur))
                .thenThrow(new ForbiddenException("fournisseur.notOwned"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ForbiddenException.class);

        verify(productFournisseurDomainService, never()).create(any(), any(), any());
    }

    @Test
    void findResponseById_should_return_when_owned() {
        ProductFournisseur pf = sample();

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productFournisseurDomainService.findById(productFournisseurId)).thenReturn(pf);

        ProductFournisseurResponse response = service.findResponseById(productFournisseurId);

        assertThat(response.id()).isEqualTo(productFournisseurId);
    }

    @Test
    void findResponseById_should_throw_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Product foreignProduct = new Product();
        foreignProduct.setId(UUID.randomUUID());
        foreignProduct.setEntreprise(other);
        ProductFournisseur foreign = sample();
        foreign.setProduct(foreignProduct);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productFournisseurDomainService.findById(productFournisseurId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.findResponseById(productFournisseurId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findAllByCurrentEntreprise_should_paginate() {
        Pageable pageable = PageRequest.of(0, 10);
        ProductFournisseurResponse item = new ProductFournisseurResponse(
                productFournisseurId,
                new ProductSummaryResponse(productId, "Pneu", "PN-195"),
                new FournisseurSummaryResponse(fournisseurId, "Pneus Maroc"),
                new BigDecimal("12.50"), "REF-FRN-001", "Maroc"
        );
        Page<ProductFournisseurResponse> page = new PageImpl<>(List.of(item), pageable, 1);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productFournisseurDomainService.findResponsesByEntrepriseId(entrepriseId, pageable)).thenReturn(page);

        Page<ProductFournisseurResponse> result = service.findAllByCurrentEntreprise(pageable);

        assertThat(result.getContent()).containsExactly(item);
    }

    @Test
    void findAllByProductId_should_check_product_then_paginate() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductFournisseurResponse> page = new PageImpl<>(List.of(), pageable, 0);

        when(productService.findById(productId)).thenReturn(product);
        when(productService.ensureBelongsToCurrentEntreprise(product)).thenReturn(product);
        when(productFournisseurDomainService.findResponsesByProductId(productId, pageable)).thenReturn(page);

        Page<ProductFournisseurResponse> result = service.findAllByProductId(productId, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void update_should_change_price_and_traceability_fields() {
        ProductFournisseur pf = sample();
        ProductFournisseurRequest request = new ProductFournisseurRequest(
                productId, fournisseurId, new BigDecimal("20.00"), "REF-NEW", "France"
        );

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productFournisseurDomainService.findById(productFournisseurId)).thenReturn(pf);
        when(productFournisseurDomainService.save(any(ProductFournisseur.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductFournisseurResponse response = service.update(productFournisseurId, request);

        assertThat(response.prixAchat()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(response.referenceFournisseur()).isEqualTo("REF-NEW");
        assertThat(response.origine()).isEqualTo("France");
        assertThat(response.product().id()).isEqualTo(productId);
        assertThat(response.fournisseur().id()).isEqualTo(fournisseurId);
    }

    @Test
    void update_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Product foreignProduct = new Product();
        foreignProduct.setId(UUID.randomUUID());
        foreignProduct.setEntreprise(other);
        ProductFournisseur foreign = sample();
        foreign.setProduct(foreignProduct);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productFournisseurDomainService.findById(productFournisseurId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.update(productFournisseurId,
                new ProductFournisseurRequest(productId, fournisseurId, new BigDecimal("1"), null, null)))
                .isInstanceOf(ForbiddenException.class);

        verify(productFournisseurDomainService, never()).save(any());
    }

    @Test
    void delete_should_remove_when_owned() {
        ProductFournisseur pf = sample();

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productFournisseurDomainService.findById(productFournisseurId)).thenReturn(pf);

        service.delete(productFournisseurId);

        verify(productFournisseurDomainService).delete(pf);
    }

    @Test
    void delete_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Product foreignProduct = new Product();
        foreignProduct.setId(UUID.randomUUID());
        foreignProduct.setEntreprise(other);
        ProductFournisseur foreign = sample();
        foreign.setProduct(foreignProduct);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(productFournisseurDomainService.findById(productFournisseurId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.delete(productFournisseurId))
                .isInstanceOf(ForbiddenException.class);

        verify(productFournisseurDomainService, never()).delete(any(ProductFournisseur.class));
    }

    @Test
    void ensurePairAvailable_should_throw_when_exists() {
        when(productFournisseurDomainService.existsByProductIdAndFournisseurId(productId, fournisseurId)).thenReturn(true);

        assertThatThrownBy(() -> service.ensurePairAvailable(productId, fournisseurId))
                .isInstanceOf(UniqueResourceException.class);
    }
}
