package org.store.reporting.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.service.CommandeAchatDomainService;
import org.store.reporting.application.dto.OwnerOverviewStatsResponse;
import org.store.reporting.application.service.IOwnerReportingService;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.domain.service.StockDomainService;
import org.store.vente.domain.service.CommandeVenteDomainService;
import org.store.vente.domain.service.FactureClientDomainService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aggregates all company-wide OWNER KPIs in a single transactional call.
 * Scopes every query by the current user's entrepriseId extracted from the JWT.
 */
@Service
@Transactional(readOnly = true)
public class OwnerReportingServiceImpl implements IOwnerReportingService {

    private final ICurrentUserService currentUserService;
    private final CommandeVenteDomainService commandeVenteDomainService;
    private final FactureClientDomainService factureClientDomainService;
    private final StockDomainService stockDomainService;
    private final CommandeAchatDomainService commandeAchatDomainService;

    public OwnerReportingServiceImpl(ICurrentUserService currentUserService,
                                     CommandeVenteDomainService commandeVenteDomainService,
                                     FactureClientDomainService factureClientDomainService,
                                     StockDomainService stockDomainService,
                                     CommandeAchatDomainService commandeAchatDomainService) {
        this.currentUserService = currentUserService;
        this.commandeVenteDomainService = commandeVenteDomainService;
        this.factureClientDomainService = factureClientDomainService;
        this.stockDomainService = stockDomainService;
        this.commandeAchatDomainService = commandeAchatDomainService;
    }

    @Override
    public OwnerOverviewStatsResponse getOwnerOverviewStats() {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay   = LocalDate.now().plusDays(1).atStartOfDay();

        long ventesTodayCount         = commandeVenteDomainService.countByEntrepriseAndDay(entrepriseId, startOfDay, endOfDay);
        var  ventesTodayTotal         = factureClientDomainService.sumMontantByEntrepriseAndDay(entrepriseId, startOfDay, endOfDay);
        long stockBelowThresholdCount = stockDomainService.countBelowThresholdByEntreprise(entrepriseId);
        long achatsEnAttente          = commandeAchatDomainService.countByEntrepriseAndStatut(entrepriseId, CommandeAchatStatut.DRAFT);
        long facturesImpayees         = factureClientDomainService.countUnpaidByEntreprise(entrepriseId);

        return new OwnerOverviewStatsResponse(
                ventesTodayCount,
                ventesTodayTotal,
                stockBelowThresholdCount,
                achatsEnAttente,
                facturesImpayees
        );
    }
}
