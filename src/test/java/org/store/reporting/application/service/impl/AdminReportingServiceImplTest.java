package org.store.reporting.application.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.dto.AbonnementStatsResponse;
import org.store.abonnement.application.dto.PaiementAbonnementStatsResponse;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.contact.application.service.IContactMessageService;
import org.store.contact.domain.enums.ContactStatut;
import org.store.entreprise.application.dto.EntrepriseCountResponse;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.magasin.application.dto.MagasinCountResponse;
import org.store.magasin.application.service.IMagasinService;
import org.store.reporting.application.dto.AdminOverviewStatsResponse;
import org.store.reporting.application.dto.PeriodReportResponse;
import org.store.users.application.service.IEmployeService;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReportingServiceImplTest {

    @Mock private IEntrepriseService entrepriseService;
    @Mock private IMagasinService magasinService;
    @Mock private IEmployeService employeService;
    @Mock private IAbonnementService abonnementService;
    @Mock private IPaiementAbonnementService paiementAbonnementService;
    @Mock private IContactMessageService contactMessageService;

    @InjectMocks
    private AdminReportingServiceImpl service;

    @Test
    void getOverviewStats_should_aggregate_grouped_counts() {
        lenient().when(entrepriseService.countAllStats()).thenReturn(new EntrepriseCountResponse(5L, 3L, 2L));
        lenient().when(magasinService.countAllStats()).thenReturn(new MagasinCountResponse(8L, 6L, 2L));
        lenient().when(abonnementService.countAllStats()).thenReturn(new AbonnementStatsResponse(4L, 1L, 2L, 0L));
        lenient().when(employeService.countAll()).thenReturn(0L);
        lenient().when(contactMessageService.countByStatut(ContactStatut.NOUVEAU)).thenReturn(0L);
        lenient().when(paiementAbonnementService.sumValidatedRevenueForYear(org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(BigDecimal.ZERO);
        when(paiementAbonnementService.countPendingFactures()).thenReturn(1L);

        AdminOverviewStatsResponse response = service.getOverviewStats();

        assertThat(response.totalEntreprises()).isEqualTo(5L);
        assertThat(response.totalEntreprisesActives()).isEqualTo(3L);
        assertThat(response.totalEntreprisesInactives()).isEqualTo(2L);
        assertThat(response.totalMagasins()).isEqualTo(8L);
        assertThat(response.totalMagasinsActifs()).isEqualTo(6L);
        assertThat(response.totalMagasinsInactifs()).isEqualTo(2L);
        assertThat(response.abonnementsActifs()).isEqualTo(4L);
        assertThat(response.abonnementsTrial()).isEqualTo(1L);
        assertThat(response.abonnementsExpires()).isEqualTo(2L);
        assertThat(response.abonnementsSuspendus()).isEqualTo(0L);
        assertThat(response.paiementsEnAttente()).isEqualTo(1L);
    }

    @Test
    void getPeriodStats_should_delegate_to_paiement_abonnement_stats() {
        lenient().when(abonnementService.countByCreatedDateRange("2026-01-01", "2026-01-31")).thenReturn(3L);
        lenient().when(paiementAbonnementService.getStatistiquesPaiement("2026-01-01", "2026-01-31"))
                .thenReturn(new PaiementAbonnementStatsResponse(5L, BigDecimal.TEN));

        PeriodReportResponse response = service.getPeriodStats("2026-01-01", "2026-01-31");

        assertThat(response.nouveauxAbonnements()).isEqualTo(3L);
        assertThat(response.paiementsValides()).isEqualTo(5L);
        assertThat(response.revenu()).isEqualByComparingTo(BigDecimal.TEN);
    }
}
