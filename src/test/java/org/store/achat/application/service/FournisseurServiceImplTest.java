package org.store.achat.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.store.achat.application.dto.FournisseurFilter;
import org.store.achat.application.dto.FournisseurRequest;
import org.store.achat.application.dto.FournisseurResponse;
import org.store.achat.application.service.impl.FournisseurServiceImpl;
import org.store.achat.domain.model.Fournisseur;
import org.store.achat.domain.service.FournisseurDomainService;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
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
class FournisseurServiceImplTest {

    @Mock private FournisseurDomainService fournisseurDomainService;
    @Mock private IEntrepriseService entrepriseService;
    @Mock private ICurrentUserService currentUserService;

    @InjectMocks
    private FournisseurServiceImpl service;

    private UUID entrepriseId;
    private UUID fournisseurId;
    private Entreprise entreprise;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        fournisseurId = UUID.randomUUID();
        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, UUID.randomUUID(), "owner", null, null, "OWNER",
                List.of("SUPPLIER_CREATE", "SUPPLIER_READ"));
    }

    private Fournisseur sample(Entreprise ent) {
        Fournisseur f = new Fournisseur();
        f.setId(fournisseurId);
        f.setNom("Pneus Maroc SARL");
        f.setEmail("contact@pneus-maroc.ma");
        f.setTelephone("+221770000000");
        f.setReference("FRN-001");
        f.setOrigine("Maroc");
        f.setEntreprise(ent);
        return f;
    }

    @Test
    void create_should_persist_and_scope_to_current_entreprise() {
        FournisseurRequest request = new FournisseurRequest(
                "Pneus Maroc SARL", null, "contact@pneus-maroc.ma", "+221770000000",
                "Casablanca", "FRN-001", "Maroc"
        );
        Fournisseur created = sample(entreprise);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(fournisseurDomainService.existsByReferenceAndEntrepriseId("FRN-001", entrepriseId)).thenReturn(false);
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(fournisseurDomainService.create(request, entreprise)).thenReturn(created);

        FournisseurResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(fournisseurId);
        assertThat(response.nom()).isEqualTo("Pneus Maroc SARL");
        assertThat(response.reference()).isEqualTo("FRN-001");
        assertThat(response.entrepriseId()).isEqualTo(entrepriseId);
    }

    @Test
    void create_should_skip_unicity_check_when_reference_blank() {
        FournisseurRequest request = new FournisseurRequest(
                "Sans Ref", null, null, null, null, "", null
        );
        Fournisseur created = sample(entreprise);
        created.setReference("");

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(entrepriseService.findById(entrepriseId)).thenReturn(entreprise);
        when(fournisseurDomainService.create(request, entreprise)).thenReturn(created);

        service.create(request);

        verify(fournisseurDomainService, never()).existsByReferenceAndEntrepriseId(any(), any());
    }

    @Test
    void create_should_throw_when_reference_already_exists() {
        FournisseurRequest request = new FournisseurRequest(
                "Doublon", null, null, null, null, "FRN-DUP", null
        );

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(fournisseurDomainService.existsByReferenceAndEntrepriseId("FRN-DUP", entrepriseId)).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(UniqueResourceException.class);

        verify(fournisseurDomainService, never()).create(any(), any());
    }

    @Test
    void findResponseById_should_return_when_owned() {
        Fournisseur fournisseur = sample(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(fournisseurDomainService.findById(fournisseurId)).thenReturn(fournisseur);

        FournisseurResponse response = service.findResponseById(fournisseurId);

        assertThat(response.id()).isEqualTo(fournisseurId);
        assertThat(response.entrepriseId()).isEqualTo(entrepriseId);
    }

    @Test
    void findResponseById_should_throw_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Fournisseur foreign = sample(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(fournisseurDomainService.findById(fournisseurId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.findResponseById(fournisseurId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findAll_should_paginate() {
        FournisseurFilter filter = new FournisseurFilter(null, null, null, null, 0, 10);
        FournisseurResponse item = new FournisseurResponse(fournisseurId, "Pneus Maroc SARL",
                null, "contact@pneus-maroc.ma", "+221770000000", "Casablanca", "FRN-001", "Maroc", entrepriseId);
        Page<FournisseurResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(fournisseurDomainService.findResponsesByFilter(filter, entrepriseId)).thenReturn(page);

        Page<FournisseurResponse> result = service.findAll(filter);

        assertThat(result.getContent()).containsExactly(item);
    }

    @Test
    void update_should_change_fields() {
        Fournisseur fournisseur = sample(entreprise);
        FournisseurRequest request = new FournisseurRequest(
                "Pneus Maroc Updated", null, "new@pneus-maroc.ma", "+221770000000",
                "Rabat", "FRN-001", "Maroc"
        );

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(fournisseurDomainService.findById(fournisseurId)).thenReturn(fournisseur);
        when(fournisseurDomainService.save(any(Fournisseur.class))).thenAnswer(inv -> inv.getArgument(0));

        FournisseurResponse response = service.update(fournisseurId, request);

        assertThat(response.nom()).isEqualTo("Pneus Maroc Updated");
        assertThat(response.email()).isEqualTo("new@pneus-maroc.ma");
        assertThat(response.adresse()).isEqualTo("Rabat");
        verify(fournisseurDomainService, never()).existsByReferenceAndEntrepriseId(any(), any());
    }

    @Test
    void update_should_check_unicity_when_reference_changes() {
        Fournisseur fournisseur = sample(entreprise);
        FournisseurRequest request = new FournisseurRequest(
                "Pneus Maroc", null, null, null, null, "FRN-NEW", null
        );

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(fournisseurDomainService.findById(fournisseurId)).thenReturn(fournisseur);
        when(fournisseurDomainService.existsByReferenceAndEntrepriseId("FRN-NEW", entrepriseId)).thenReturn(true);

        assertThatThrownBy(() -> service.update(fournisseurId, request))
                .isInstanceOf(UniqueResourceException.class);

        verify(fournisseurDomainService, never()).save(any());
    }

    @Test
    void update_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Fournisseur foreign = sample(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(fournisseurDomainService.findById(fournisseurId)).thenReturn(foreign);

        FournisseurRequest updateReq = new FournisseurRequest("x", null, null, null, null, null, null);

        assertThatThrownBy(() -> service.update(fournisseurId, updateReq))
                .isInstanceOf(ForbiddenException.class);

        verify(fournisseurDomainService, never()).save(any());
    }

    @Test
    void delete_should_remove_when_owned() {
        Fournisseur fournisseur = sample(entreprise);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(fournisseurDomainService.findById(fournisseurId)).thenReturn(fournisseur);

        service.delete(fournisseurId);

        verify(fournisseurDomainService).delete(fournisseur);
    }

    @Test
    void delete_should_throw_forbidden_when_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        Fournisseur foreign = sample(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(fournisseurDomainService.findById(fournisseurId)).thenReturn(foreign);

        assertThatThrownBy(() -> service.delete(fournisseurId))
                .isInstanceOf(ForbiddenException.class);

        verify(fournisseurDomainService, never()).delete(any(Fournisseur.class));
    }

    @Test
    void ensureReferenceAvailable_should_skip_when_null_or_blank() {
        service.ensureReferenceAvailable(null, entrepriseId);
        service.ensureReferenceAvailable("", entrepriseId);
        service.ensureReferenceAvailable("   ", entrepriseId);

        verify(fournisseurDomainService, never()).existsByReferenceAndEntrepriseId(any(), any());
    }

    @Test
    void ensureReferenceAvailable_should_throw_when_taken() {
        when(fournisseurDomainService.existsByReferenceAndEntrepriseId(eq("FRN-X"), eq(entrepriseId))).thenReturn(true);

        assertThatThrownBy(() -> service.ensureReferenceAvailable("FRN-X", entrepriseId))
                .isInstanceOf(UniqueResourceException.class);
    }
}
