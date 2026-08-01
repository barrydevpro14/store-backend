package org.store.reporting.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.service.ICommandeAchatService;
import org.store.achat.application.service.IFactureAchatService;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.reporting.application.dto.OwnerOverviewStatsResponse;
import org.store.reporting.application.service.IOwnerReportingService;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.service.IStockService;
import org.store.vente.application.service.IFactureClientService;

import java.util.UUID;

/**
 * Agrège les KPIs snapshot de l'entreprise pour le dashboard OWNER.
 * Tous les counts sont globaux (sans filtre de date) — le reporting daté passe par l'endpoint magasin.
 */
@Service
@Transactional(readOnly = true)
public class OwnerReportingServiceImpl implements IOwnerReportingService {

    private final ICurrentUserService currentUserService;
    private final IFactureClientService factureClientService;
    private final IStockService stockService;
    private final ICommandeAchatService commandeAchatService;
    private final IFactureAchatService factureAchatService;

    public OwnerReportingServiceImpl(ICurrentUserService currentUserService,
                                     IFactureClientService factureClientService,
                                     IStockService stockService,
                                     ICommandeAchatService commandeAchatService,
                                     IFactureAchatService factureAchatService) {
        this.currentUserService = currentUserService;
        this.factureClientService = factureClientService;
        this.stockService = stockService;
        this.commandeAchatService = commandeAchatService;
        this.factureAchatService = factureAchatService;
    }

    /** Retourne les KPIs snapshot de l'entreprise du caller (stock, achats, factures — sans filtre date). */
    @Override
    public OwnerOverviewStatsResponse getOwnerOverviewStats() {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();

        long produitsBasSeuil      = stockService.countBelowThresholdByEntreprise(entrepriseId);
        long achatsEnAttente       = commandeAchatService.countByEntrepriseAndStatut(entrepriseId, CommandeAchatStatut.DRAFT);
        long facturesVenteImpayees = factureClientService.countUnpaidByEntreprise(entrepriseId);
        long facturesAchatImpayees = factureAchatService.countUnpaidByEntreprise(entrepriseId);

        return new OwnerOverviewStatsResponse(
                produitsBasSeuil,
                achatsEnAttente,
                facturesVenteImpayees,
                facturesAchatImpayees
        );
    }
}
