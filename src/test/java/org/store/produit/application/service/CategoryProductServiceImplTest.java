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
import org.store.common.exceptions.ForbiddenException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.produit.application.dto.CategoryProductRequest;
import org.store.produit.application.dto.CategoryProductResponse;
import org.store.produit.application.service.impl.CategoryProductServiceImpl;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.service.CategoryProductDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryProductServiceImplTest {

    @Mock private CategoryProductDomainService categoryProductDomainService;
    @Mock private IEntrepriseService entrepriseService;
    @Mock private ICurrentUserService currentUserService;

    @InjectMocks
    private CategoryProductServiceImpl service;

    private UUID entrepriseId;
    private UUID categoryId;
    private Entreprise entreprise;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), entrepriseId, UUID.randomUUID(), "owner", "PROPRIETAIRE",
                List.of("CATEGORY_PRODUCT_CREATE", "CATEGORY_PRODUCT_READ"));
    }

    private CategoryProduct sampleCategory(Entreprise ent) {
        CategoryProduct c = new CategoryProduct();
        c.setId(categoryId);
        c.setLibelle("Pneus");
        c.setDescription("Cat. pneus toutes saisons");
        c.setEntreprise(ent);
        return c;
    }

    @Test
    void create_should_persist_and_scope_to_current_entreprise() {
        CategoryProductRequest request = new CategoryProductRequest("Pneus", "Cat. pneus toutes saisons");
        CategoryProduct created = sampleCategory(entreprise);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(categoryProductDomainService.existsByLibelleAndEntrepriseId("Pneus", entrepriseId)).thenReturn(false);
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(categoryProductDomainService.create(request, entreprise)).thenReturn(created);

        CategoryProductResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(categoryId);
        assertThat(response.libelle()).isEqualTo("Pneus");
        assertThat(response.entrepriseId()).isEqualTo(entrepriseId);
    }

    @Test
    void create_should_throw_when_libelle_already_exists() {
        CategoryProductRequest request = new CategoryProductRequest("Pneus", null);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(categoryProductDomainService.existsByLibelleAndEntrepriseId("Pneus", entrepriseId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UniqueResourceException.class);

        verify(categoryProductDomainService, never()).create(any(), any());
    }

    @Test
    void findResponseById_should_return_when_owned() {
        CategoryProduct category = sampleCategory(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(categoryProductDomainService.findById(categoryId)).thenReturn(category);

        CategoryProductResponse response = service.findResponseById(categoryId);

        assertThat(response.id()).isEqualTo(categoryId);
        assertThat(response.entrepriseId()).isEqualTo(entrepriseId);
    }

    @Test
    void findResponseById_should_throw_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        CategoryProduct foreign = sampleCategory(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(categoryProductDomainService.findById(categoryId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.findResponseById(categoryId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findAllByCurrentEntreprise_should_paginate() {
        Pageable pageable = PageRequest.of(0, 10);
        CategoryProductResponse sample = new CategoryProductResponse(categoryId, "Pneus", "desc", entrepriseId);
        Page<CategoryProductResponse> page = new PageImpl<>(List.of(sample), pageable, 1);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(categoryProductDomainService.findResponsesByEntrepriseId(entrepriseId, pageable)).thenReturn(page);

        Page<CategoryProductResponse> result = service.findAllByCurrentEntreprise(pageable);

        assertThat(result.getContent()).containsExactly(sample);
    }

    @Test
    void update_should_change_libelle_and_description() {
        CategoryProduct category = sampleCategory(entreprise);
        CategoryProductRequest request = new CategoryProductRequest("Filtres", "Cat. filtres");

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(categoryProductDomainService.findById(categoryId)).thenReturn(category);
        when(categoryProductDomainService.existsByLibelleAndEntrepriseId("Filtres", entrepriseId)).thenReturn(false);
        when(categoryProductDomainService.save(any(CategoryProduct.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryProductResponse response = service.update(categoryId, request);

        assertThat(response.libelle()).isEqualTo("Filtres");
        assertThat(response.description()).isEqualTo("Cat. filtres");
    }

    @Test
    void update_should_skip_unicity_check_when_libelle_unchanged() {
        CategoryProduct category = sampleCategory(entreprise);
        CategoryProductRequest request = new CategoryProductRequest("Pneus", "Nouvelle description");

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(categoryProductDomainService.findById(categoryId)).thenReturn(category);
        when(categoryProductDomainService.save(any(CategoryProduct.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(categoryId, request);

        verify(categoryProductDomainService, never()).existsByLibelleAndEntrepriseId(any(), any());
    }

    @Test
    void update_should_throw_when_new_libelle_taken() {
        CategoryProduct category = sampleCategory(entreprise);
        CategoryProductRequest request = new CategoryProductRequest("Filtres", "x");

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(categoryProductDomainService.findById(categoryId)).thenReturn(category);
        when(categoryProductDomainService.existsByLibelleAndEntrepriseId("Filtres", entrepriseId)).thenReturn(true);

        assertThatThrownBy(() -> service.update(categoryId, request))
                .isInstanceOf(UniqueResourceException.class);

        verify(categoryProductDomainService, never()).save(any());
    }

    @Test
    void update_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        CategoryProduct foreign = sampleCategory(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(categoryProductDomainService.findById(categoryId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.update(categoryId, new CategoryProductRequest("x", null)))
                .isInstanceOf(ForbiddenException.class);

        verify(categoryProductDomainService, never()).save(any());
    }

    @Test
    void delete_should_remove_when_owned() {
        CategoryProduct category = sampleCategory(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(categoryProductDomainService.findById(categoryId)).thenReturn(category);

        service.delete(categoryId);

        verify(categoryProductDomainService).delete(category);
    }

    @Test
    void delete_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        CategoryProduct foreign = sampleCategory(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(categoryProductDomainService.findById(categoryId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.delete(categoryId))
                .isInstanceOf(ForbiddenException.class);

        verify(categoryProductDomainService, never()).delete(any(CategoryProduct.class));
    }

    @Test
    void ensureBelongsToCurrentEntreprise_should_return_category_when_owned() {
        CategoryProduct category = sampleCategory(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThat(service.ensureBelongsToCurrentEntreprise(category)).isSameAs(category);
    }

    @Test
    void ensureBelongsToCurrentEntreprise_should_throw_when_other() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        CategoryProduct foreign = sampleCategory(other);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThatThrownBy(() -> service.ensureBelongsToCurrentEntreprise(foreign))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void ensureLibelleAvailable_should_throw_when_taken() {
        when(categoryProductDomainService.existsByLibelleAndEntrepriseId(eq("Pneus"), eq(entrepriseId))).thenReturn(true);

        assertThatThrownBy(() -> service.ensureLibelleAvailable("Pneus", entrepriseId))
                .isInstanceOf(UniqueResourceException.class);
    }
}
