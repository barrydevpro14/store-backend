package org.store.magasin.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.tools.DateHelper;
import org.store.magasin.application.dto.MagasinStatsResponse;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.application.service.IMagasinStatsService;
import org.store.magasin.domain.model.Magasin;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.application.service.IStockService;
import org.store.users.application.service.IEmployeService;
import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.service.ICaisseService;
import org.store.vente.application.service.IClientService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Aggregates current-month magasin KPIs (employees, clients, stock, revenue)
 * from dedicated application services — no domain service crossed directly.
 */
@Service
@Transactional(readOnly = true)
public class MagasinStatsServiceImpl implements IMagasinStatsService {

    private final IMagasinService magasinService;
    private final IEmployeService employeService;
    private final IClientService clientService;
    private final IStockService stockService;
    private final ICaisseService caisseService;

    public MagasinStatsServiceImpl(IMagasinService magasinService,
                                   IEmployeService employeService,
                                   IClientService clientService,
                                   IStockService stockService,
                                   ICaisseService caisseService) {
        this.magasinService = magasinService;
        this.employeService = employeService;
        this.clientService = clientService;
        this.stockService = stockService;
        this.caisseService = caisseService;
    }

    @Override
    public MagasinStatsResponse getStats(UUID magasinId) {
        Magasin magasin = magasinService.ensureBelongsToCurrentEntreprise(magasinService.findById(magasinId));
        UUID entrepriseId = magasin.getEntreprise().getId();

        YearMonth currentMonth = YearMonth.now();
        LocalDate startOfMonth = currentMonth.atDay(1);
        LocalDate endOfMonth   = currentMonth.atEndOfMonth();

        long nombreEmployes = employeService.countByMagasinId(magasinId);
        long nombreClients  = clientService.countByEntrepriseId(entrepriseId);

        StockValuationResponse valuation = stockService.computeValuation(magasinId);

        BigDecimal revenuMois = caisseService
                .getResume(new CaisseResumeFilter(magasinId, DateHelper.format(startOfMonth), DateHelper.format(endOfMonth)))
                .totalCommandes();

        return new MagasinStatsResponse(
                nombreEmployes,
                nombreClients,
                valuation.nombreLignes(),
                valuation.valeurTotale(),
                revenuMois != null ? revenuMois : BigDecimal.ZERO
        );
    }
}
