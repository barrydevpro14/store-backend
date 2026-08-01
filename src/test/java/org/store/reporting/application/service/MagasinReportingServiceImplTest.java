package org.store.reporting.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.application.service.ICommandeAchatService;
import org.store.achat.application.service.IFactureAchatService;
import org.store.common.dto.DataCountResponse;
import org.store.reporting.application.dto.MagasinDashboardStatsResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MagasinReportingServiceImplTest {

    @Mock private ICaisseService caisseService;
    @Mock private IStockService stockService;
    @Mock private ICommandeAchatService commandeAchatService;
    @Mock private IFactureClientService factureClientService;
    @Mock private IFactureAchatService factureAchatService;

    @InjectMocks
    private MagasinReportingServiceImpl service;

    private final UUID magasinId = UUID.randomUUID();
    private final LocalDate from = LocalDate.of(2025, 7, 1);
    private final LocalDate to   = LocalDate.of(2025, 7, 31);

    private MagasinOverviewFilter sampleFilter() {
        return new MagasinOverviewFilter(magasinId, from, to);
    }

    private CaisseResumeResponse resume(long n, BigDecimal total, BigDecimal paiements) {
        return new CaisseResumeResponse(magasinId, from, to, n, 0L, total, paiements, List.of(), List.of());
    }

    private void stubOverview(long n, BigDecimal total, BigDecimal paiements,
                              long achats, long facturesVente, long facturesAchat) {
        when(caisseService.getResume(any())).thenReturn(resume(n, total, paiements));
        when(commandeAchatService.countDraftByMagasinAndDateBetween(eq(magasinId), eq(from), eq(to))).thenReturn(achats);
        when(factureClientService.countUnpaidByMagasinAndDateBetween(eq(magasinId), eq(from), eq(to))).thenReturn(facturesVente);
        when(factureAchatService.countUnpaidByMagasinAndDateBetween(eq(magasinId), eq(from), eq(to))).thenReturn(facturesAchat);
    }

    // ── getOverview ──────────────────────────────────────────────────────────

    @Test
    void getOverview_should_compute_ticket_moyen_when_commandes_present() {
        stubOverview(4L, new BigDecimal("200.00"), new BigDecimal("180.00"), 2L, 3L, 1L);

        MagasinOverviewStatsResponse result = service.getOverview(sampleFilter());

        assertThat(result.nombreCommandeVentes()).isEqualTo(4L);
        assertThat(result.montantTotalCommandeVentes()).isEqualByComparingTo("200.00");
        assertThat(result.totalPaiementVentes()).isEqualByComparingTo("180.00");
        assertThat(result.ticketMoyen()).isEqualByComparingTo("50.00");
        assertThat(result.achatsEnAttente()).isEqualTo(2L);
        assertThat(result.facturesVenteImpayees()).isEqualTo(3L);
        assertThat(result.facturesAchatImpayees()).isEqualTo(1L);
    }

    @Test
    void getOverview_should_return_zero_ticket_moyen_when_no_commandes() {
        stubOverview(0L, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0L);

        MagasinOverviewStatsResponse result = service.getOverview(sampleFilter());

        assertThat(result.nombreCommandeVentes()).isZero();
        assertThat(result.ticketMoyen()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getOverview_should_pass_correct_dates_to_caisse_service() {
        stubOverview(1L, new BigDecimal("100.00"), new BigDecimal("100.00"), 0L, 0L, 0L);

        service.getOverview(sampleFilter());

        ArgumentCaptor<CaisseResumeFilter> captor = ArgumentCaptor.forClass(CaisseResumeFilter.class);
        verify(caisseService).getResume(captor.capture());
        assertThat(captor.getValue().magasinId()).isEqualTo(magasinId);
        assertThat(captor.getValue().from()).isEqualTo("2025-07-01");
        assertThat(captor.getValue().to()).isEqualTo("2025-07-31");
    }

    @Test
    void getOverview_should_delegate_date_filtered_counts() {
        stubOverview(2L, new BigDecimal("400.00"), new BigDecimal("400.00"), 5L, 7L, 4L);

        service.getOverview(sampleFilter());

        verify(commandeAchatService).countDraftByMagasinAndDateBetween(magasinId, from, to);
        verify(factureClientService).countUnpaidByMagasinAndDateBetween(magasinId, from, to);
        verify(factureAchatService).countUnpaidByMagasinAndDateBetween(magasinId, from, to);
    }

    // ── getDashboardStats ────────────────────────────────────────────────────

    @Test
    void getDashboardStats_should_aggregate_snapshot_kpis() {
        when(stockService.computeValuation(magasinId))
                .thenReturn(new StockValuationResponse(magasinId, new BigDecimal("12000.00"), 10L));
        when(stockService.countBelowThresholdByCurrentEntreprise(magasinId)).thenReturn(3L);
        when(commandeAchatService.countDraft(magasinId)).thenReturn(new DataCountResponse(4L));
        when(factureClientService.countAllUnpaid(magasinId)).thenReturn(new DataCountResponse(2L));
        when(factureAchatService.countUnpaidByMagasin(magasinId)).thenReturn(1L);

        MagasinDashboardStatsResponse result = service.getDashboardStats(magasinId);

        assertThat(result.valeurStock()).isEqualByComparingTo("12000.00");
        assertThat(result.produitsBasSeuil()).isEqualTo(3L);
        assertThat(result.achatsEnAttente()).isEqualTo(4L);
        assertThat(result.facturesVenteImpayees()).isEqualTo(2L);
        assertThat(result.facturesAchatImpayees()).isEqualTo(1L);
    }
}
