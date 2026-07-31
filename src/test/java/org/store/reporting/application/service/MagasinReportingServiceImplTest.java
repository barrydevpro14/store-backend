package org.store.reporting.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.application.service.ICommandeAchatService;
import org.store.common.dto.DataCountResponse;
import org.store.reporting.application.dto.MagasinOverviewFilter;
import org.store.reporting.application.dto.MagasinOverviewStatsResponse;
import org.store.reporting.application.service.impl.MagasinReportingServiceImpl;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.application.service.IStockService;
import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.CaisseResumeResponse;
import org.store.vente.application.service.ICaisseService;
import org.store.vente.application.service.IFactureClientService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MagasinReportingServiceImplTest {

    @Mock private ICaisseService caisseService;
    @Mock private IStockService stockService;
    @Mock private ICommandeAchatService commandeAchatService;
    @Mock private IFactureClientService factureClientService;

    @InjectMocks
    private MagasinReportingServiceImpl service;

    private final UUID magasinId  = UUID.randomUUID();
    private final LocalDate startDate = LocalDate.of(2025, 7, 1);
    private final LocalDate endDate   = LocalDate.of(2025, 7, 31);

    private MagasinOverviewFilter sampleFilter() {
        return new MagasinOverviewFilter(magasinId, startDate, endDate);
    }

    private CaisseResumeResponse resume(long nombreCommandes, BigDecimal total, BigDecimal paiements) {
        return new CaisseResumeResponse(magasinId, startDate, endDate, nombreCommandes, 0L, total, paiements, List.of(), List.of());
    }

    private StockValuationResponse valuation(BigDecimal valeur) {
        return new StockValuationResponse(magasinId, valeur, 10L);
    }

    private void stubDependencies(long nombreCommandes,
                                  BigDecimal totalCommandes,
                                  BigDecimal totalPaiements,
                                  BigDecimal valeurStock,
                                  long achats,
                                  long factures,
                                  long sousSeuil) {
        when(caisseService.getResume(any())).thenReturn(resume(nombreCommandes, totalCommandes, totalPaiements));
        when(stockService.computeValuation(magasinId)).thenReturn(valuation(valeurStock));
        when(commandeAchatService.countDraft(magasinId)).thenReturn(new DataCountResponse(achats));
        when(factureClientService.countAllUnpaid(magasinId)).thenReturn(new DataCountResponse(factures));
        when(stockService.countBelowThresholdByCurrentEntreprise(magasinId)).thenReturn(sousSeuil);
    }

    @Test
    void getOverview_should_compute_ticket_moyen_when_commandes_present() {
        stubDependencies(4L, new BigDecimal("200.00"), new BigDecimal("180.00"),
                new BigDecimal("5000.00"), 2L, 3L, 1L);

        MagasinOverviewStatsResponse result = service.getOverview(sampleFilter());

        assertThat(result.nombreCommandes()).isEqualTo(4L);
        assertThat(result.totalCommandes()).isEqualByComparingTo("200.00");
        assertThat(result.totalPaiements()).isEqualByComparingTo("180.00");
        assertThat(result.ticketMoyen()).isEqualByComparingTo("50.00");
        assertThat(result.valeurStock()).isEqualByComparingTo("5000.00");
        assertThat(result.achatsEnAttente()).isEqualTo(2L);
        assertThat(result.facturesImpayees()).isEqualTo(3L);
        assertThat(result.produitsBasSeuil()).isEqualTo(1L);
    }

    @Test
    void getOverview_should_return_zero_ticket_moyen_when_no_commandes() {
        stubDependencies(0L, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("3000.00"), 0L, 1L, 0L);

        MagasinOverviewStatsResponse result = service.getOverview(sampleFilter());

        assertThat(result.nombreCommandes()).isZero();
        assertThat(result.ticketMoyen()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getOverview_should_pass_correct_dates_to_caisse_service() {
        stubDependencies(1L, new BigDecimal("100.00"), new BigDecimal("100.00"),
                BigDecimal.ZERO, 0L, 0L, 0L);

        service.getOverview(sampleFilter());

        ArgumentCaptor<CaisseResumeFilter> captor = ArgumentCaptor.forClass(CaisseResumeFilter.class);
        verify(caisseService).getResume(captor.capture());

        CaisseResumeFilter capturedFilter = captor.getValue();
        assertThat(capturedFilter.magasinId()).isEqualTo(magasinId);
        assertThat(capturedFilter.from()).isEqualTo("2025-07-01");
        assertThat(capturedFilter.to()).isEqualTo("2025-07-31");
    }

    @Test
    void getOverview_should_delegate_all_count_calls() {
        stubDependencies(2L, new BigDecimal("400.00"), new BigDecimal("400.00"),
                new BigDecimal("1000.00"), 5L, 7L, 3L);

        service.getOverview(sampleFilter());

        verify(commandeAchatService).countDraft(magasinId);
        verify(factureClientService).countAllUnpaid(magasinId);
        verify(stockService).countBelowThresholdByCurrentEntreprise(magasinId);
        verify(stockService).computeValuation(magasinId);
    }
}
