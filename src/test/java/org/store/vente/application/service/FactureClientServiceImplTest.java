package org.store.vente.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.dto.FactureClientFilter;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.application.service.impl.FactureClientServiceImpl;
import org.store.vente.domain.service.FactureClientDomainService;

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
class FactureClientServiceImplTest {

    @Mock private FactureClientDomainService factureClientDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private FactureClientServiceImpl service;

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
                "vendeur1", "SELLER", List.of("SALE_READ"));
    }

    private FactureClientResponse sampleFacture(UUID id) {
        return new FactureClientResponse(
                id, "FAC-VTE-001", StatutFacture.NON_PAYEE,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                LocalDate.of(2026, 5, 16), LocalDate.of(2026, 5, 30), UUID.randomUUID()
        );
    }

    @Test
    void findAllByCurrentEntreprise_should_validate_filter_and_delegate() {
        FactureClientFilter filter = new FactureClientFilter(magasinId, null, null, null, null, null, null, null, null, null, null, 0, 10);
        Page<FactureClientResponse> page = new PageImpl<>(List.of(sampleFacture(UUID.randomUUID())));

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(factureClientDomainService.findResponsesByFilter(filter, entrepriseId)).thenReturn(page);

        Page<FactureClientResponse> result = service.findAllByCurrentEntreprise(filter);

        assertThat(result.getContent()).hasSize(1);
        verify(validatorService).validate(filter);
    }

    @Test
    void findAllByCurrentEntreprise_should_propagate_forbidden_when_magasin_not_accessible() {
        FactureClientFilter filter = new FactureClientFilter(magasinId, null, null, null, null, null, null, null, null, null, null, 0, 10);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.findAllByCurrentEntreprise(filter))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findResponseById_should_return_response_when_found_in_entreprise_scope() {
        UUID factureId = UUID.randomUUID();
        FactureClientResponse response = sampleFacture(factureId);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(factureClientDomainService.findResponseById(factureId, entrepriseId))
                .thenReturn(Optional.of(response));

        FactureClientResponse result = service.findResponseById(factureId);

        assertThat(result.id()).isEqualTo(factureId);
        assertThat(result.numero()).isEqualTo("FAC-VTE-001");
    }

    @Test
    void findResponseById_should_throw_notFound_when_absent_or_other_entreprise() {
        UUID factureId = UUID.randomUUID();

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(factureClientDomainService.findResponseById(factureId, entrepriseId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findResponseById(factureId))
                .isInstanceOf(EntityException.class);
    }
}
