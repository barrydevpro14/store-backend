package org.store.reporting.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.application.service.ICommandeAchatService;
import org.store.achat.application.service.IFactureAchatService;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.reporting.application.dto.OwnerOverviewStatsResponse;
import org.store.reporting.application.service.impl.OwnerReportingServiceImpl;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.service.IStockService;
import org.store.vente.application.service.IFactureClientService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OwnerReportingServiceImplTest {

    @Mock private ICurrentUserService currentUserService;
    @Mock private IFactureClientService factureClientService;
    @Mock private IStockService stockService;
    @Mock private ICommandeAchatService commandeAchatService;
    @Mock private IFactureAchatService factureAchatService;

    @InjectMocks
    private OwnerReportingServiceImpl service;

    private final UUID entrepriseId = UUID.randomUUID();

    private UserPrincipal owner() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, null,
                "owner", null, null, "OWNER", List.of("OWNER_OVERVIEW"));
    }

    private void stubDependencies(long seuil, long achats, long facturesVente, long facturesAchat) {
        when(currentUserService.getCurrent()).thenReturn(owner());
        when(stockService.countBelowThresholdByEntreprise(entrepriseId)).thenReturn(seuil);
        when(commandeAchatService.countByEntrepriseAndStatut(entrepriseId, CommandeAchatStatut.DRAFT)).thenReturn(achats);
        when(factureClientService.countUnpaidByEntreprise(entrepriseId)).thenReturn(facturesVente);
        when(factureAchatService.countUnpaidByEntreprise(entrepriseId)).thenReturn(facturesAchat);
    }

    @Test
    void getOwnerOverviewStats_should_aggregate_snapshot_kpis() {
        stubDependencies(3L, 2L, 4L, 7L);

        OwnerOverviewStatsResponse result = service.getOwnerOverviewStats();

        assertThat(result.produitsBasSeuil()).isEqualTo(3L);
        assertThat(result.achatsEnAttente()).isEqualTo(2L);
        assertThat(result.facturesVenteImpayees()).isEqualTo(4L);
        assertThat(result.facturesAchatImpayees()).isEqualTo(7L);
    }

    @Test
    void getOwnerOverviewStats_should_scope_all_queries_by_entrepriseId() {
        stubDependencies(0L, 0L, 0L, 0L);

        service.getOwnerOverviewStats();

        verify(stockService).countBelowThresholdByEntreprise(entrepriseId);
        verify(commandeAchatService).countByEntrepriseAndStatut(entrepriseId, CommandeAchatStatut.DRAFT);
        verify(factureClientService).countUnpaidByEntreprise(entrepriseId);
        verify(factureAchatService).countUnpaidByEntreprise(entrepriseId);
    }
}
