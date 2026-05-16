package org.store.vente.application.service;

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
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.application.service.impl.PaiementVenteServiceImpl;
import org.store.vente.domain.service.PaiementVenteDomainService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaiementVenteServiceImplTest {

    @Mock private PaiementVenteDomainService paiementVenteDomainService;
    @Mock private ICurrentUserService currentUserService;

    @InjectMocks
    private PaiementVenteServiceImpl service;

    private UUID entrepriseId;
    private UUID factureId;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        factureId = UUID.randomUUID();
    }

    private UserPrincipal currentUser() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, UUID.randomUUID(),
                "vendeur1", "VENDEUR", List.of("SALE_READ"));
    }

    @Test
    void findByFactureId_should_delegate_with_currentEntreprise() {
        Pageable pageable = PageRequest.of(0, 10);
        PaiementVenteResponse paiement = new PaiementVenteResponse(
                UUID.randomUUID(), new BigDecimal("500.00"), LocalDate.of(2026, 5, 16),
                MoyenPaiement.CASH, factureId
        );
        Page<PaiementVenteResponse> page = new PageImpl<>(List.of(paiement), pageable, 1);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(paiementVenteDomainService.findResponsesByFactureId(factureId, entrepriseId, pageable))
                .thenReturn(page);

        Page<PaiementVenteResponse> result = service.findByFactureId(factureId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().montant()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void findByFactureId_should_return_empty_page_when_facture_belongs_to_other_entreprise() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaiementVenteResponse> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(paiementVenteDomainService.findResponsesByFactureId(factureId, entrepriseId, pageable))
                .thenReturn(emptyPage);

        Page<PaiementVenteResponse> result = service.findByFactureId(factureId, pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
