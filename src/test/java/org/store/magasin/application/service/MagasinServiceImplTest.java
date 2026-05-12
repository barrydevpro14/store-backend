package org.store.magasin.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.exceptions.ForbiddenException;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.magasin.application.dto.MagasinResponse;
import org.store.magasin.domain.model.Magasin;
import org.store.magasin.domain.service.MagasinDomainService;
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
class MagasinServiceImplTest {

    @Mock private MagasinDomainService magasinDomainService;
    @Mock private IEntrepriseService entrepriseService;
    @Mock private ICurrentUserService currentUserService;

    @InjectMocks
    private MagasinServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private Entreprise entreprise;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), entrepriseId, magasinId, "owner", "PROPRIETAIRE",
                List.of("PROPRIETAIRE_ACCESS"));
    }

    private Magasin magasinIn(Entreprise ent) {
        Magasin m = new Magasin();
        m.setId(magasinId);
        m.setNom("Magasin Centre");
        m.setAdresse("Dakar Centre");
        m.setActif(true);
        m.setEntreprise(ent);
        return m;
    }

    @Test
    void should_create_magasin_with_passed_entreprise() {
        MagasinRequest request = new MagasinRequest("Magasin Centre", "Dakar Centre");
        when(magasinDomainService.save(any(Magasin.class))).thenAnswer(inv -> inv.getArgument(0));

        Magasin result = service.create(request, entreprise);

        ArgumentCaptor<Magasin> captor = ArgumentCaptor.forClass(Magasin.class);
        verify(magasinDomainService).save(captor.capture());
        Magasin saved = captor.getValue();
        assertThat(saved.getEntreprise()).isSameAs(entreprise);
        assertThat(saved.getNom()).isEqualTo("Magasin Centre");
        assertThat(saved.isActif()).isTrue();
        assertThat(result).isSameAs(saved);
    }

    @Test
    void create_response_should_scope_to_current_entreprise() {
        MagasinRequest request = new MagasinRequest("Magasin Plage", "Saly");
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(magasinDomainService.save(any(Magasin.class))).thenAnswer(inv -> {
            Magasin m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        MagasinResponse response = service.create(request);

        assertThat(response.entrepriseId()).isEqualTo(entrepriseId);
        assertThat(response.nom()).isEqualTo("Magasin Plage");
        assertThat(response.actif()).isTrue();
    }

    @Test
    void findById_should_delegate_to_domain_service() {
        Magasin magasin = magasinIn(entreprise);
        when(magasinDomainService.findById(magasinId)).thenReturn(magasin);

        assertThat(service.findById(magasinId)).isSameAs(magasin);
    }

    @Test
    void findResponseById_should_return_response_when_owned() {
        Magasin magasin = magasinIn(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(magasinDomainService.findById(magasinId)).thenReturn(magasin);

        MagasinResponse response = service.findResponseById(magasinId);

        assertThat(response.id()).isEqualTo(magasinId);
        assertThat(response.entrepriseId()).isEqualTo(entrepriseId);
    }

    @Test
    void findResponseById_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Magasin foreign = magasinIn(other);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(magasinDomainService.findById(magasinId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.findResponseById(magasinId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findAllByCurrentEntreprise_should_paginate() {
        Pageable pageable = PageRequest.of(0, 10);
        MagasinResponse first = new MagasinResponse(UUID.randomUUID(), "A", "Adr", true, entrepriseId);
        Page<MagasinResponse> page = new PageImpl<>(List.of(first), pageable, 1);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(magasinDomainService.findResponsesByEntrepriseId(entrepriseId, pageable)).thenReturn(page);

        Page<MagasinResponse> result = service.findAllByCurrentEntreprise(pageable);

        assertThat(result.getContent()).containsExactly(first);
    }

    @Test
    void update_should_change_nom_and_adresse() {
        Magasin magasin = magasinIn(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(magasinDomainService.findById(magasinId)).thenReturn(magasin);
        when(magasinDomainService.save(any(Magasin.class))).thenAnswer(inv -> inv.getArgument(0));

        MagasinResponse response = service.update(magasinId, new MagasinRequest("Nouveau", "Nouvelle adresse"));

        assertThat(response.nom()).isEqualTo("Nouveau");
        assertThat(response.adresse()).isEqualTo("Nouvelle adresse");
    }

    @Test
    void update_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Magasin foreign = magasinIn(other);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(magasinDomainService.findById(magasinId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.update(magasinId, new MagasinRequest("x", "y")))
                .isInstanceOf(ForbiddenException.class);

        verify(magasinDomainService, never()).save(any());
    }

    @Test
    void activate_should_set_actif_true() {
        Magasin magasin = magasinIn(entreprise);
        magasin.setActif(false);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(magasinDomainService.findById(magasinId)).thenReturn(magasin);
        when(magasinDomainService.save(any(Magasin.class))).thenAnswer(inv -> inv.getArgument(0));

        MagasinResponse response = service.activate(magasinId);

        assertThat(response.actif()).isTrue();
    }

    @Test
    void ensureBelongsToCurrentEntreprise_should_return_magasin_when_owned() {
        Magasin magasin = magasinIn(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThat(service.ensureBelongsToCurrentEntreprise(magasin)).isSameAs(magasin);
    }

    @Test
    void ensureBelongsToCurrentEntreprise_should_throw_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Magasin foreign = magasinIn(other);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThatThrownBy(() -> service.ensureBelongsToCurrentEntreprise(foreign))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void ensureAccessibleByCurrentUser_should_pass_when_proprietaire_owns_entreprise() {
        Magasin magasin = magasinIn(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThat(service.ensureAccessibleByCurrentUser(magasin)).isSameAs(magasin);
    }

    @Test
    void ensureAccessibleByCurrentUser_should_throw_when_proprietaire_targets_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Magasin foreign = magasinIn(other);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());

        assertThatThrownBy(() -> service.ensureAccessibleByCurrentUser(foreign))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void ensureAccessibleByCurrentUser_should_pass_when_non_proprietaire_targets_his_magasin() {
        Magasin magasin = magasinIn(entreprise);
        UserPrincipal manager = new UserPrincipal(UUID.randomUUID(), entrepriseId, magasinId,
                "manager", "MANAGER", List.of("EMPLOYE_ACCESS"));
        when(currentUserService.getCurrent()).thenReturn(manager);

        assertThat(service.ensureAccessibleByCurrentUser(magasin)).isSameAs(magasin);
    }

    @Test
    void ensureAccessibleByCurrentUser_should_throw_when_non_proprietaire_targets_other_magasin() {
        Magasin other = magasinIn(entreprise);
        other.setId(UUID.randomUUID());
        UserPrincipal manager = new UserPrincipal(UUID.randomUUID(), entrepriseId, magasinId,
                "manager", "MANAGER", List.of("EMPLOYE_ACCESS"));
        when(currentUserService.getCurrent()).thenReturn(manager);

        assertThatThrownBy(() -> service.ensureAccessibleByCurrentUser(other))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deactivate_should_set_actif_false() {
        Magasin magasin = magasinIn(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(magasinDomainService.findById(magasinId)).thenReturn(magasin);
        when(magasinDomainService.save(any(Magasin.class))).thenAnswer(inv -> inv.getArgument(0));

        MagasinResponse response = service.deactivate(magasinId);

        assertThat(response.actif()).isFalse();
    }
}
