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
import org.store.produit.application.dto.QualityRequest;
import org.store.produit.application.dto.QualityResponse;
import org.store.produit.application.service.impl.QualityServiceImpl;
import org.store.produit.domain.model.Quality;
import org.store.produit.domain.service.QualityDomainService;
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
class QualityServiceImplTest {

    @Mock private QualityDomainService qualityDomainService;
    @Mock private IEntrepriseService entrepriseService;
    @Mock private ICurrentUserService currentUserService;

    @InjectMocks
    private QualityServiceImpl service;

    private UUID entrepriseId;
    private UUID qualityId;
    private Entreprise entreprise;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        qualityId = UUID.randomUUID();
        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, UUID.randomUUID(), "owner", "OWNER",
                List.of("QUALITY_CREATE", "QUALITY_READ"));
    }

    private Quality sampleQuality(Entreprise ent) {
        Quality q = new Quality();
        q.setId(qualityId);
        q.setLibelle("Premium");
        q.setDescription("Qualité haut de gamme");
        q.setEntreprise(ent);
        return q;
    }

    @Test
    void create_should_persist_and_scope_to_current_entreprise() {
        QualityRequest request = new QualityRequest("Premium", "Qualité haut de gamme");
        Quality created = sampleQuality(entreprise);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(qualityDomainService.existsByLibelleAndEntrepriseId("Premium", entrepriseId)).thenReturn(false);
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(qualityDomainService.create(request, entreprise)).thenReturn(created);

        QualityResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(qualityId);
        assertThat(response.libelle()).isEqualTo("Premium");
        assertThat(response.entrepriseId()).isEqualTo(entrepriseId);
    }

    @Test
    void create_should_throw_when_libelle_already_exists() {
        QualityRequest request = new QualityRequest("Premium", null);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(qualityDomainService.existsByLibelleAndEntrepriseId("Premium", entrepriseId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UniqueResourceException.class);

        verify(qualityDomainService, never()).create(any(), any());
    }

    @Test
    void findResponseById_should_return_when_owned() {
        Quality quality = sampleQuality(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(qualityDomainService.findById(qualityId)).thenReturn(quality);

        QualityResponse response = service.findResponseById(qualityId);

        assertThat(response.id()).isEqualTo(qualityId);
        assertThat(response.entrepriseId()).isEqualTo(entrepriseId);
    }

    @Test
    void findResponseById_should_throw_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Quality foreign = sampleQuality(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(qualityDomainService.findById(qualityId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.findResponseById(qualityId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findAllByCurrentEntreprise_should_paginate() {
        Pageable pageable = PageRequest.of(0, 10);
        QualityResponse sample = new QualityResponse(qualityId, "Premium", "desc", entrepriseId);
        Page<QualityResponse> page = new PageImpl<>(List.of(sample), pageable, 1);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(qualityDomainService.findResponsesByEntrepriseId(entrepriseId, pageable)).thenReturn(page);

        Page<QualityResponse> result = service.findAllByCurrentEntreprise(pageable);

        assertThat(result.getContent()).containsExactly(sample);
    }

    @Test
    void update_should_change_libelle_and_description() {
        Quality quality = sampleQuality(entreprise);
        QualityRequest request = new QualityRequest("Standard", "Qualité standard");

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(qualityDomainService.findById(qualityId)).thenReturn(quality);
        when(qualityDomainService.existsByLibelleAndEntrepriseId("Standard", entrepriseId)).thenReturn(false);
        when(qualityDomainService.save(any(Quality.class))).thenAnswer(inv -> inv.getArgument(0));

        QualityResponse response = service.update(qualityId, request);

        assertThat(response.libelle()).isEqualTo("Standard");
        assertThat(response.description()).isEqualTo("Qualité standard");
    }

    @Test
    void update_should_skip_unicity_check_when_libelle_unchanged() {
        Quality quality = sampleQuality(entreprise);
        QualityRequest request = new QualityRequest("Premium", "Nouvelle description");

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(qualityDomainService.findById(qualityId)).thenReturn(quality);
        when(qualityDomainService.save(any(Quality.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(qualityId, request);

        verify(qualityDomainService, never()).existsByLibelleAndEntrepriseId(any(), any());
    }

    @Test
    void update_should_throw_when_new_libelle_taken() {
        Quality quality = sampleQuality(entreprise);
        QualityRequest request = new QualityRequest("Standard", "x");

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(qualityDomainService.findById(qualityId)).thenReturn(quality);
        when(qualityDomainService.existsByLibelleAndEntrepriseId("Standard", entrepriseId)).thenReturn(true);

        assertThatThrownBy(() -> service.update(qualityId, request))
                .isInstanceOf(UniqueResourceException.class);

        verify(qualityDomainService, never()).save(any());
    }

    @Test
    void update_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Quality foreign = sampleQuality(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(qualityDomainService.findById(qualityId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.update(qualityId, new QualityRequest("x", null)))
                .isInstanceOf(ForbiddenException.class);

        verify(qualityDomainService, never()).save(any());
    }

    @Test
    void delete_should_remove_when_owned() {
        Quality quality = sampleQuality(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(qualityDomainService.findById(qualityId)).thenReturn(quality);

        service.delete(qualityId);

        verify(qualityDomainService).delete(quality);
    }

    @Test
    void delete_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Quality foreign = sampleQuality(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(qualityDomainService.findById(qualityId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.delete(qualityId))
                .isInstanceOf(ForbiddenException.class);

        verify(qualityDomainService, never()).delete(any(Quality.class));
    }

    @Test
    void ensureBelongsToCurrentEntreprise_should_return_quality_when_owned() {
        Quality quality = sampleQuality(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThat(service.ensureBelongsToCurrentEntreprise(quality)).isSameAs(quality);
    }

    @Test
    void ensureBelongsToCurrentEntreprise_should_throw_when_other() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Quality foreign = sampleQuality(other);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThatThrownBy(() -> service.ensureBelongsToCurrentEntreprise(foreign))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void ensureLibelleAvailable_should_throw_when_taken() {
        when(qualityDomainService.existsByLibelleAndEntrepriseId(eq("Premium"), eq(entrepriseId))).thenReturn(true);

        assertThatThrownBy(() -> service.ensureLibelleAvailable("Premium", entrepriseId))
                .isInstanceOf(UniqueResourceException.class);
    }
}
