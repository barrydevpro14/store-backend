package org.store.plateforme.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.dto.RevenuPeriodFilter;
import org.store.abonnement.application.service.IRevenuService;
import org.store.plateforme.application.dto.PlateformePeriodFilter;
import org.store.plateforme.application.dto.PlateformePeriodReportResponse;
import org.store.plateforme.application.service.impl.PlateformeReportingServiceImpl;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlateformeReportingServiceImplTest {

    @Mock private IRevenuService revenuService;
    @Mock private IDepensePlateformeService depensePlateformeService;
    @InjectMocks private PlateformeReportingServiceImpl service;

    @Test
    void getPeriodReport_should_compute_benefice_as_revenu_minus_depenses_globally() {
        PlateformePeriodFilter filter = new PlateformePeriodFilter("2026-08-01", "2026-08-31", null, null);
        when(revenuService.getTotalForPeriod(new RevenuPeriodFilter("2026-08-01", "2026-08-31", null, null))).thenReturn(new BigDecimal("1000000.00"));
        when(depensePlateformeService.computeTotal("2026-08-01", "2026-08-31", null)).thenReturn(new BigDecimal("300000.00"));

        PlateformePeriodReportResponse response = service.getPeriodReport(filter);

        assertThat(response.revenu()).isEqualByComparingTo("1000000.00");
        assertThat(response.depensesPlateforme()).isEqualByComparingTo("300000.00");
        assertThat(response.benefice()).isEqualByComparingTo("700000.00");
    }

    @Test
    void getPeriodReport_should_scope_both_operands_by_the_same_countryId() {
        UUID countryId = UUID.randomUUID();
        PlateformePeriodFilter filter = new PlateformePeriodFilter("2026-08-01", "2026-08-31", countryId, null);
        when(revenuService.getTotalForPeriod(new RevenuPeriodFilter("2026-08-01", "2026-08-31", countryId, null))).thenReturn(new BigDecimal("400000.00"));
        when(depensePlateformeService.computeTotal("2026-08-01", "2026-08-31", countryId)).thenReturn(new BigDecimal("100000.00"));

        PlateformePeriodReportResponse response = service.getPeriodReport(filter);

        assertThat(response.benefice()).isEqualByComparingTo("300000.00");
    }

    @Test
    void getPeriodReport_with_entrepriseId_should_leave_depenses_unaffected() {
        UUID entrepriseId = UUID.randomUUID();
        PlateformePeriodFilter filter = new PlateformePeriodFilter("2026-08-01", "2026-08-31", null, entrepriseId);
        when(revenuService.getTotalForPeriod(new RevenuPeriodFilter("2026-08-01", "2026-08-31", null, entrepriseId))).thenReturn(new BigDecimal("50000.00"));
        when(depensePlateformeService.computeTotal("2026-08-01", "2026-08-31", null)).thenReturn(new BigDecimal("300000.00"));

        PlateformePeriodReportResponse response = service.getPeriodReport(filter);

        assertThat(response.depensesPlateforme()).isEqualByComparingTo("300000.00");
        assertThat(response.benefice()).isEqualByComparingTo("-250000.00");
    }
}
