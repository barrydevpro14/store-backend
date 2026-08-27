package org.store.reporting.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.AbonnementStatsResponse;
import org.store.abonnement.application.dto.PaiementAbonnementStatsResponse;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.contact.application.service.IContactMessageService;
import org.store.contact.domain.enums.ContactStatut;
import org.store.entreprise.application.dto.EntrepriseCountResponse;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.magasin.application.dto.MagasinCountResponse;
import org.store.magasin.application.dto.MagasinStatsRow;
import org.store.magasin.application.service.IMagasinService;
import org.store.reporting.application.dto.AdminOverviewStatsResponse;
import org.store.reporting.application.dto.PeriodReportResponse;
import org.store.reporting.application.service.IAdminReportingService;
import org.store.users.application.service.IEmployeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Aggregates all admin overview KPI counts in a single transactional call,
 * replacing multiple separate API calls from the frontend.
 */
@Service
@Transactional(readOnly = true)
public class AdminReportingServiceImpl implements IAdminReportingService {

    private final IEntrepriseService entrepriseService;
    private final IMagasinService magasinService;
    private final IEmployeService employeService;
    private final IAbonnementService abonnementService;
    private final IPaiementAbonnementService paiementAbonnementService;
    private final IContactMessageService contactMessageService;

    public AdminReportingServiceImpl(IEntrepriseService entrepriseService,
                                     IMagasinService magasinService,
                                     IEmployeService employeService,
                                     IAbonnementService abonnementService,
                                     IPaiementAbonnementService paiementAbonnementService,
                                     IContactMessageService contactMessageService) {
        this.entrepriseService = entrepriseService;
        this.magasinService = magasinService;
        this.employeService = employeService;
        this.abonnementService = abonnementService;
        this.paiementAbonnementService = paiementAbonnementService;
        this.contactMessageService = contactMessageService;
    }

    @Override
    public AdminOverviewStatsResponse getOverviewStats() {
        int currentYear = LocalDate.now().getYear();

        EntrepriseCountResponse entrepriseStats = entrepriseService.countAllStats();
        MagasinCountResponse magasinStats       = magasinService.countAllStats();
        AbonnementStatsResponse abonnementStats = abonnementService.countAllStats();
        long totalEmployes             = employeService.countAll();
        long contactMessagesNouveaux   = contactMessageService.countByStatut(ContactStatut.NOUVEAU);
        BigDecimal revenueYtd          = paiementAbonnementService.sumValidatedRevenueForYear(currentYear);
        long paiementsEnAttente        = paiementAbonnementService.countPendingFactures();

        return new AdminOverviewStatsResponse(
                entrepriseStats.total(),
                entrepriseStats.actifs(),
                entrepriseStats.inactifs(),
                magasinStats.total(),
                magasinStats.actifs(),
                magasinStats.inactifs(),
                totalEmployes,
                abonnementStats.actifs(),
                abonnementStats.trial(),
                abonnementStats.expires(),
                abonnementStats.suspendus(),
                contactMessagesNouveaux,
                revenueYtd != null ? revenueYtd : BigDecimal.ZERO,
                paiementsEnAttente
        );
    }

    @Override
    public List<MagasinStatsRow> getEntrepriseStats(UUID entrepriseId) {
        return magasinService.findStatsByEntrepriseId(entrepriseId);
    }

    @Override
    public PeriodReportResponse getPeriodStats(String startDate, String endDate) {
        long nouveauxAbonnements = abonnementService.countByCreatedDateRange(startDate, endDate);
        PaiementAbonnementStatsResponse paiementAbonnementStats =paiementAbonnementService.getStatistiquesPaiement(startDate,endDate);

        return new PeriodReportResponse(
                nouveauxAbonnements,
                paiementAbonnementStats.valides(),
                paiementAbonnementStats.revenu()
        );
    }
}
