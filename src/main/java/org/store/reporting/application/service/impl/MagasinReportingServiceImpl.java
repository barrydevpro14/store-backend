package org.store.reporting.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.service.ICommandeAchatService;
import org.store.reporting.application.dto.MagasinOverviewFilter;
import org.store.reporting.application.dto.MagasinOverviewStatsResponse;
import org.store.reporting.application.service.IMagasinReportingService;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.application.service.IStockService;
import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.CaisseResumeResponse;
import org.store.vente.application.service.ICaisseService;
import org.store.vente.application.service.IFactureClientService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Aggregates all magasin-scoped KPIs (sales, stock, purchases, invoices)
 * for a given period into a single response.
 */
@Service
@Transactional(readOnly = true)
public class MagasinReportingServiceImpl implements IMagasinReportingService {

    private final ICaisseService caisseService;
    private final IStockService stockService;
    private final ICommandeAchatService commandeAchatService;
    private final IFactureClientService factureClientService;

    public MagasinReportingServiceImpl(ICaisseService caisseService,
                                       IStockService stockService,
                                       ICommandeAchatService commandeAchatService,
                                       IFactureClientService factureClientService) {
        this.caisseService = caisseService;
        this.stockService = stockService;
        this.commandeAchatService = commandeAchatService;
        this.factureClientService = factureClientService;
    }

    @Override
    public MagasinOverviewStatsResponse getOverview(MagasinOverviewFilter magasinOverviewFilter) {
        UUID magasinId = magasinOverviewFilter.magasinId();

        CaisseResumeResponse resume = caisseService.getResume(
                new CaisseResumeFilter(magasinId, magasinOverviewFilter.startDateAsString(), magasinOverviewFilter.endDateAsString())
        );

        StockValuationResponse valuation = stockService.computeValuation(magasinId);

        long achatsEnAttente  = commandeAchatService.countDraft(magasinId).totalElements();
        long facturesImpayees = factureClientService.countAllUnpaid(magasinId).totalElements();
        long produitsBasSeuil = stockService.countBelowThresholdByCurrentEntreprise(magasinId);

        long nombreCommandes      = resume.nombreCommandes();
        BigDecimal totalCommandes = resume.totalCommandes();
        BigDecimal totalPaiements = resume.totalPaiements();

        BigDecimal ticketMoyen = nombreCommandes > 0
                ? totalCommandes.divide(BigDecimal.valueOf(nombreCommandes), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new MagasinOverviewStatsResponse(
                nombreCommandes,
                totalCommandes,
                totalPaiements,
                ticketMoyen,
                valuation.valeurTotale(),
                produitsBasSeuil,
                achatsEnAttente,
                facturesImpayees
        );
    }
}
