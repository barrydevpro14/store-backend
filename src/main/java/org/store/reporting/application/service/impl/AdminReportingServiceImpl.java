package org.store.reporting.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.abonnement.domain.service.PaiementAbonnementDomainService;
import org.store.entreprise.domain.service.EntrepriseDomainService;
import org.store.magasin.domain.service.MagasinDomainService;
import org.store.reporting.application.dto.AdminOverviewStatsResponse;
import org.store.reporting.application.service.IAdminReportingService;
import org.store.users.domain.service.EmployeDomainService;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Aggregates all admin overview KPI counts in a single transactional call,
 * replacing multiple separate API calls from the frontend.
 */
@Service
@Transactional(readOnly = true)
public class AdminReportingServiceImpl implements IAdminReportingService {

    private final EntrepriseDomainService entrepriseDomainService;
    private final AbonnementDomainService abonnementDomainService;
    private final PaiementAbonnementDomainService paiementAbonnementDomainService;
    private final MagasinDomainService magasinDomainService;
    private final EmployeDomainService employeDomainService;

    public AdminReportingServiceImpl(EntrepriseDomainService entrepriseDomainService,
                                     AbonnementDomainService abonnementDomainService,
                                     PaiementAbonnementDomainService paiementAbonnementDomainService,
                                     MagasinDomainService magasinDomainService,
                                     EmployeDomainService employeDomainService) {
        this.entrepriseDomainService = entrepriseDomainService;
        this.abonnementDomainService = abonnementDomainService;
        this.paiementAbonnementDomainService = paiementAbonnementDomainService;
        this.magasinDomainService = magasinDomainService;
        this.employeDomainService = employeDomainService;
    }

    @Override
    public AdminOverviewStatsResponse getOverviewStats() {
        int currentYear = LocalDate.now().getYear();

        long totalEntreprises   = entrepriseDomainService.count();
        long totalMagasins      = magasinDomainService.countByEntrepriseId().values().stream().mapToLong(Long::longValue).sum();
        long totalEmployes      = employeDomainService.countByEntrepriseId().values().stream().mapToLong(Long::longValue).sum();
        long abonnementsActifs  = abonnementDomainService.countByStatut(AbonnementStatut.ACTIF);
        long abonnementsTrial   = abonnementDomainService.countByStatut(AbonnementStatut.TRIAL);
        long abonnementsExpires = abonnementDomainService.countByStatut(AbonnementStatut.EXPIRE);
        long abonnementsSuspend = abonnementDomainService.countByStatut(AbonnementStatut.SUSPENDU);
        long paiementsEnAttente = paiementAbonnementDomainService.countByStatut(StatutPaiementAbonnement.EN_ATTENTE_VALIDATION);
        long paiementsRejetes   = paiementAbonnementDomainService.countByStatut(StatutPaiementAbonnement.REJETE);
        BigDecimal revenueYtd   = paiementAbonnementDomainService.sumValidatedRevenueForYear(currentYear);

        return new AdminOverviewStatsResponse(
                totalEntreprises,
                totalMagasins,
                totalEmployes,
                abonnementsActifs,
                abonnementsTrial,
                abonnementsExpires,
                abonnementsSuspend,
                paiementsEnAttente,
                paiementsRejetes,
                revenueYtd != null ? revenueYtd : BigDecimal.ZERO
        );
    }
}
