package org.store.stock.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.dto.MarginReportFilter;
import org.store.stock.application.dto.MarginReportResponse;
import org.store.stock.application.service.impl.MarginReportServiceImpl;
import org.store.stock.domain.service.SortieStockDomainService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarginReportServiceImplTest {

    @Mock private SortieStockDomainService sortieStockDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private MarginReportServiceImpl service;

    private UUID magasinId;
    private UUID entrepriseId;
    private Entreprise entreprise;
    private Magasin magasin;

    @BeforeEach
    void setUp() {
        magasinId = UUID.randomUUID();
        entrepriseId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), entrepriseId, null, "owner", "PROPRIETAIRE", List.of("REPORT_STOCK"));
    }

    @Test
    void compute_should_validate_check_access_and_delegate() {
        MarginReportFilter filter = new MarginReportFilter(magasinId, null, null, "2026-05-01", "2026-05-14");
        MarginReportResponse expected = new MarginReportResponse(new BigDecimal("2750.00"), 150L, 2L);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(sortieStockDomainService.computeMargin(eq(filter), eq(entrepriseId))).thenReturn(expected);

        MarginReportResponse response = service.compute(filter);

        verify(validatorService).validate(filter);
        assertThat(response.margeTotale()).isEqualByComparingTo(new BigDecimal("2750.00"));
        assertThat(response.quantiteVendueTotale()).isEqualTo(150L);
        assertThat(response.nombreSorties()).isEqualTo(2L);
    }

    @Test
    void compute_should_propagate_forbidden_when_magasin_not_accessible() {
        MarginReportFilter filter = new MarginReportFilter(magasinId, null, null, null, null);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.compute(filter))
                .isInstanceOf(ForbiddenException.class);
    }
}
