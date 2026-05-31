package org.store.entreprise.application.service;

import org.store.entreprise.application.service.impl.EntrepriseServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.store.common.exceptions.ForbiddenException;
import org.store.entreprise.application.dto.EntrepriseRequest;
import org.store.entreprise.application.dto.EntrepriseResponse;
import org.store.entreprise.domain.model.Entreprise;
import org.store.entreprise.domain.service.EntrepriseDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.users.domain.model.Proprietaire;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntrepriseServiceImplTest {

    @Mock private EntrepriseDomainService entrepriseDomainService;
    @Mock private org.store.common.service.IUploadFileService uploadFileService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private org.store.common.service.ValidatorService validatorService;

    @InjectMocks
    private EntrepriseServiceImpl service;

    private UUID entrepriseId;
    private Entreprise entreprise;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
        entreprise.setSigle("ACME");
        entreprise.setRaisonSociale("ACME SARL");
        entreprise.setActif(true);
    }

    private UserPrincipal proprietaire(UUID entId) {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entId, UUID.randomUUID(), "owner", null, null, "OWNER",
                List.of("OWNER_ACCESS"));
    }

    @Test
    void create_should_delegate_to_domain_service() {
        EntrepriseRequest request = new EntrepriseRequest(
                "ACME", "ACME SARL", "NINEA-123", "RCCM-456", "Dakar"
        , null);
        Proprietaire proprietaire = new Proprietaire();
        Entreprise expected = new Entreprise();
        when(entrepriseDomainService.create(request, proprietaire)).thenReturn(expected);

        Entreprise result = service.create(request, proprietaire);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void findCurrentUserEntreprise_should_return_response() {
        when(currentUserService.getCurrent()).thenReturn(proprietaire(entrepriseId));
        when(entrepriseDomainService.findById(entrepriseId)).thenReturn(entreprise);

        EntrepriseResponse response = service.findCurrentUserEntreprise();

        assertThat(response.id()).isEqualTo(entrepriseId);
        assertThat(response.sigle()).isEqualTo("ACME");
    }

    @Test
    void updateCurrentUserEntreprise_should_apply_changes() {
        EntrepriseRequest request = new EntrepriseRequest("NEW", "NEW SARL", "N2", "R2", "Adr2", null);
        when(currentUserService.getCurrent()).thenReturn(proprietaire(entrepriseId));
        when(entrepriseDomainService.findById(entrepriseId)).thenReturn(entreprise);
        when(entrepriseDomainService.save(any(Entreprise.class))).thenAnswer(inv -> inv.getArgument(0));

        EntrepriseResponse response = service.updateCurrentUserEntreprise(request);

        assertThat(response.sigle()).isEqualTo("NEW", null);
        assertThat(response.raisonSociale()).isEqualTo("NEW SARL");
        assertThat(response.ninea()).isEqualTo("N2");
    }

    @Test
    void uploadCurrentUserLogo_should_build_pieceJointe_and_set() {
        org.springframework.mock.web.MockMultipartFile file = new org.springframework.mock.web.MockMultipartFile(
                "file", "logo.png", "image/png", new byte[]{1, 2, 3});
        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
        org.store.common.model.PieceJointe piece = new org.store.common.model.PieceJointe();
        piece.setDocument(new byte[]{1, 2, 3});
        piece.setContentType("image/png");

        when(currentUserService.getCurrent()).thenReturn(proprietaire(entrepriseId));
        when(entrepriseDomainService.findById(entrepriseId)).thenReturn(entreprise);
        when(uploadFileService.buildImage(file)).thenReturn(piece);
        when(entrepriseDomainService.setLogo(entreprise, piece)).thenReturn(entreprise);

        service.uploadCurrentUserLogo(file);

        verify(entrepriseDomainService).setLogo(entreprise, piece);
    }

    @Test
    void getCurrentUserLogo_should_throw_when_no_logo() {
        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
        entreprise.setLogo(null);
        when(currentUserService.getCurrent()).thenReturn(proprietaire(entrepriseId));
        when(entrepriseDomainService.findById(entrepriseId)).thenReturn(entreprise);

        assertThatThrownBy(() -> service.getCurrentUserLogo())
                .isInstanceOf(org.store.common.exceptions.EntityException.class);
    }

    @Test
    void findAll_should_delegate_filter_to_domain_service() {
        org.store.entreprise.application.dto.EntrepriseFilter filter =
                new org.store.entreprise.application.dto.EntrepriseFilter("ACME", null, null, null, true, null, null, 0, 10);
        EntrepriseResponse sample = new EntrepriseResponse(entrepriseId, "ACME", "ACME SARL",
                "N", "R", "A", null, true, true, null);
        Page<EntrepriseResponse> page = new PageImpl<>(List.of(sample), PageRequest.of(0, 10), 1);
        when(entrepriseDomainService.findResponsesByFilter(filter)).thenReturn(page);

        Page<EntrepriseResponse> result = service.findAll(filter);

        assertThat(result.getContent()).containsExactly(sample);
        verify(validatorService).validate(filter);
    }

    @Test
    void update_should_apply_changes_via_domain_service() {
        Entreprise existing = new Entreprise();
        existing.setId(entrepriseId);
        when(entrepriseDomainService.findById(entrepriseId)).thenReturn(existing);
        when(entrepriseDomainService.save(any(Entreprise.class))).thenAnswer(inv -> inv.getArgument(0));

        EntrepriseResponse response = service.update(entrepriseId,
                new EntrepriseRequest("NEW", "NEW SARL", "N2", "R2", "Adr", null));

        assertThat(response.sigle()).isEqualTo("NEW", null);
        assertThat(response.raisonSociale()).isEqualTo("NEW SARL");
    }

    @Test
    void findResponseById_should_wrap_entity() {
        when(entrepriseDomainService.findById(entrepriseId)).thenReturn(entreprise);

        EntrepriseResponse response = service.findResponseById(entrepriseId);

        assertThat(response.id()).isEqualTo(entrepriseId);
    }

    @Test
    void activate_should_set_actif_true() {
        entreprise.setActif(false);
        when(entrepriseDomainService.findById(entrepriseId)).thenReturn(entreprise);
        when(entrepriseDomainService.save(any(Entreprise.class))).thenAnswer(inv -> inv.getArgument(0));

        EntrepriseResponse response = service.activate(entrepriseId);

        assertThat(response.actif()).isTrue();
    }

    @Test
    void deactivate_should_set_actif_false() {
        when(entrepriseDomainService.findById(entrepriseId)).thenReturn(entreprise);
        when(entrepriseDomainService.save(any(Entreprise.class))).thenAnswer(inv -> inv.getArgument(0));

        EntrepriseResponse response = service.deactivate(entrepriseId);

        assertThat(response.actif()).isFalse();
    }

    @Test
    void ensureBelongsToCurrentUser_should_return_entreprise_when_owned() {
        when(currentUserService.getCurrent()).thenReturn(proprietaire(entrepriseId));

        assertThat(service.ensureBelongsToCurrentUser(entreprise)).isSameAs(entreprise);
    }

    @Test
    void ensureBelongsToCurrentUser_should_throw_when_other_entreprise() {
        when(currentUserService.getCurrent()).thenReturn(proprietaire(UUID.randomUUID()));

        assertThatThrownBy(() -> service.ensureBelongsToCurrentUser(entreprise))
                .isInstanceOf(ForbiddenException.class);
    }
}
