package org.store.vente.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.store.common.dto.UserSummaryResponse;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.dto.CommandeVenteFilter;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.application.service.impl.CommandeVenteServiceImpl;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.service.CommandeVenteDomainService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandeVenteServiceImplTest {

    @Mock private CommandeVenteDomainService commandeVenteDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private CommandeVenteServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private Magasin magasin;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();

        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);
    }

    private UserPrincipal currentUser() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, magasinId,
                "vendeur1", null, null, "SELLER", List.of("SALE_READ"));
    }

    private CommandeVenteResponse sampleResponse() {
        return new CommandeVenteResponse(
                UUID.randomUUID(), "VTE-AUTO-001", CommandeVenteStatut.VALIDATE,
                null, null, LocalDate.of(2026, 5, 16),
                new BigDecimal("1300.00"), BigDecimal.ZERO,new BigDecimal("1300.00"),
                null, "2026-05-16 10:00:00"
        );
    }

    @Test
    void findAllByCurrentEntreprise_should_validate_filter_and_delegate_to_domain() {
        CommandeVenteFilter filter = new CommandeVenteFilter(magasinId, null, null, null, null, null, null, null, null, null, 0, 10);
        Page<CommandeVenteResponse> page = new PageImpl<>(List.of(sampleResponse()));

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(commandeVenteDomainService.findResponsesByFilter(filter, entrepriseId)).thenReturn(page);

        Page<CommandeVenteResponse> result = service.findAllByCurrentEntreprise(filter);

        assertThat(result.getContent()).hasSize(1);
        verify(validatorService).validate(filter);
    }

    @Test
    void findAllByCurrentEntreprise_should_propagate_forbidden_when_magasin_not_accessible() {
        CommandeVenteFilter filter = new CommandeVenteFilter(magasinId, null, null, null, null, null, null, null, null, null, 0, 10);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.findAllByCurrentEntreprise(filter))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findResponseById_should_return_response_with_user_when_found_in_entreprise_scope() {
        UUID commandeId = UUID.randomUUID();
        CommandeVenteResponse responseWithUser = new CommandeVenteResponse(
                commandeId, "VTE-AUTO-002", CommandeVenteStatut.VALIDATE,
                null, new UserSummaryResponse(UUID.randomUUID(), "Diop Awa"),
                LocalDate.of(2026, 5, 16),
                new BigDecimal("1300.00"), BigDecimal.ZERO,new BigDecimal("1300.00"),
                null, "2026-05-16 10:00:00"
        );

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(commandeVenteDomainService.findResponseById(commandeId, entrepriseId))
                .thenReturn(Optional.of(responseWithUser));

        CommandeVenteResponse result = service.findResponseById(commandeId);

        assertThat(result.id()).isEqualTo(commandeId);
        assertThat(result.user()).isNotNull();
        assertThat(result.user().nomComplet()).isEqualTo("Diop Awa");
    }

    @Test
    void findResponseById_should_throw_notFound_when_absent_or_other_entreprise() {
        UUID commandeId = UUID.randomUUID();

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(commandeVenteDomainService.findResponseById(commandeId, entrepriseId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findResponseById(commandeId))
                .isInstanceOf(EntityException.class);
    }
}
