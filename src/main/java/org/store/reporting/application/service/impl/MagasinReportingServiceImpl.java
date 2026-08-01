package org.store.reporting.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.service.ICommandeAchatService;
import org.store.achat.application.service.IFactureAchatService;
import org.store.reporting.application.dto.MagasinDashboardStatsResponse;
import org.store.reporting.application.dto.MagasinOverviewFilter;
import org.store.reporting.application.dto.MagasinOverviewStatsResponse;
import org.store.reporting.application.dto.MagasinVentesStatsResponse;
import org.store.reporting.application.service.IMagasinReportingService;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.application.service.IStockService;
import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.CaisseResumeResponse;
import org.store.vente.application.service.ICaisseService;
import org.store.vente.application.service.IFactureClientService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Agrège les KPIs du magasin pour le dashboard (snapshot global) et pour le reporting (filtrés par date métier).
 */
@Service
@Transactional(readOnly = true)
public class MagasinReportingServiceImpl implements IMagasinReportingService {

    private final ICaisseService caisseService;
    private final IStockService stockService;
    private final ICommandeAchatService commandeAchatService;
    private final IFactureClientService factureClientService;
    private final IFactureAchatService factureAchatService;

    public MagasinReportingServiceImpl(ICaisseService caisseService,
                                       IStockService stockService,
                                       ICommandeAchatService commandeAchatService,
                                       IFactureClientService factureClientService,
                                       IFactureAchatService factureAchatService) {
        this.caisseService = caisseService;
        this.stockService = stockService;
        this.commandeAchatService = commandeAchatService;
        this.factureClientService = factureClientService;
        this.factureAchatService = factureAchatService;
    }

    @Override
    public MagasinOverviewStatsResponse getOverview(MagasinOverviewFilter filter) {
        UUID magasinId = filter.magasinId();
        LocalDate from = filter.startDate();
        LocalDate to   = filter.endDate();

        CaisseResumeResponse resume = caisseService.getResume(
                new CaisseResumeFilter(magasinId, filter.startDateAsString(), filter.endDateAsString())
        );

        long nombreCommandeVentes      = resume.nombreCommandes();
        BigDecimal montantTotalCommandeVentes = resume.totalCommandes();
        BigDecimal totalPaiementVentes        = resume.totalPaiements();

        BigDecimal ticketMoyen = nombreCommandeVentes > 0
                ? montantTotalCommandeVentes.divide(BigDecimal.valueOf(nombreCommandeVentes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long achatsEnAttente      = commandeAchatService.countDraftByMagasinAndDateBetween(magasinId, from, to);
        long facturesVenteImpayees = factureClientService.countUnpaidByMagasinAndDateBetween(magasinId, from, to);
        long facturesAchatImpayees = factureAchatService.countUnpaidByMagasinAndDateBetween(magasinId, from, to);

        return new MagasinOverviewStatsResponse(
                nombreCommandeVentes,
                montantTotalCommandeVentes,
                totalPaiementVentes,
                ticketMoyen,
                achatsEnAttente,
                facturesVenteImpayees,
                facturesAchatImpayees
        );
    }

    @Override
    public MagasinVentesStatsResponse getVentesStats(MagasinOverviewFilter filter) {
        UUID magasinId = filter.magasinId();
        LocalDate from = filter.startDate();
        LocalDate to   = filter.endDate();

        CaisseResumeResponse resume = caisseService.getResume(
                new CaisseResumeFilter(magasinId, filter.startDateAsString(), filter.endDateAsString())
        );

        long nombreCommandeVentes            = resume.nombreCommandes();
        BigDecimal montantTotalCommandeVentes = resume.totalCommandes();
        BigDecimal totalPaiementVentes        = resume.totalPaiements();

        BigDecimal ticketMoyen = nombreCommandeVentes > 0
                ? montantTotalCommandeVentes.divide(BigDecimal.valueOf(nombreCommandeVentes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long facturesVenteImpayees = factureClientService.countUnpaidByMagasinAndDateBetween(magasinId, from, to);

        return new MagasinVentesStatsResponse(
                nombreCommandeVentes,
                montantTotalCommandeVentes,
                totalPaiementVentes,
                ticketMoyen,
                facturesVenteImpayees
        );
    }

    @Override
    public MagasinDashboardStatsResponse getDashboardStats(UUID magasinId) {
        StockValuationResponse valuation = stockService.computeValuation(magasinId);

        long produitsBasSeuil      = stockService.countBelowThresholdByCurrentEntreprise(magasinId);
        long achatsEnAttente       = commandeAchatService.countDraft(magasinId).totalElements();
        long facturesVenteImpayees = factureClientService.countAllUnpaid(magasinId).totalElements();
        long facturesAchatImpayees = factureAchatService.countUnpaidByMagasin(magasinId);

        return new MagasinDashboardStatsResponse(
                valuation.valeurTotale(),
                produitsBasSeuil,
                achatsEnAttente,
                facturesVenteImpayees,
                facturesAchatImpayees
        );
    }
}
