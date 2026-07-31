package org.store.magasin.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.magasin.application.dto.MagasinStatsResponse;
import org.store.magasin.application.service.impl.MagasinStatsServiceImpl;
import org.store.magasin.domain.model.Magasin;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.application.service.IStockService;
import org.store.users.application.service.IEmployeService;
import org.store.vente.application.dto.CaisseResumeResponse;
import org.store.vente.application.service.ICaisseService;
import org.store.vente.application.service.IClientService;
import org.store.entreprise.domain.model.Entreprise;

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
class MagasinStatsServiceImplTest {

    @Mock private IMagasinService magasinService;
    @Mock private IEmployeService employeService;
    @Mock private IClientService clientService;
    @Mock private IStockService stockService;
    @Mock private ICaisseService caisseService;

    @InjectMocks
    private MagasinStatsServiceImpl service;

    private final UUID magasinId   = UUID.randomUUID();
    private final UUID entrepriseId = UUID.randomUUID();

    private Magasin magasinWithEntreprise() {
        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        Magasin magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);

        return magasin;
    }

    private CaisseResumeResponse caisse(BigDecimal totalCommandes) {
        return new CaisseResumeResponse(magasinId, LocalDate.now(), LocalDate.now(),
                5L, 10L, totalCommandes, totalCommandes, List.of(), List.of());
    }

    private void stubAll(long employes, long clients, BigDecimal valeurStock, long lignes, BigDecimal revenu) {
        Magasin magasin = magasinWithEntreprise();
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureBelongsToCurrentEntreprise(magasin)).thenReturn(magasin);
        when(employeService.countByMagasinId(magasinId)).thenReturn(employes);
        when(clientService.countByEntrepriseId(entrepriseId)).thenReturn(clients);
        when(stockService.computeValuation(eq(magasinId))).thenReturn(new StockValuationResponse(magasinId, valeurStock, lignes));
        when(caisseService.getResume(any())).thenReturn(caisse(revenu));
    }

    @Test
    void getStats_should_aggregate_all_kpis() {
        stubAll(5L, 12L, new BigDecimal("8000.00"), 20L, new BigDecimal("1500.00"));

        MagasinStatsResponse result = service.getStats(magasinId);

        assertThat(result.nombreEmployes()).isEqualTo(5L);
        assertThat(result.nombreClients()).isEqualTo(12L);
        assertThat(result.nombreProduitsEnStock()).isEqualTo(20L);
        assertThat(result.valeurTotaleStock()).isEqualByComparingTo("8000.00");
        assertThat(result.revenuMoisCourant()).isEqualByComparingTo("1500.00");
    }

    @Test
    void getStats_should_default_revenu_to_zero_when_null() {
        stubAll(2L, 3L, new BigDecimal("500.00"), 5L, null);

        MagasinStatsResponse result = service.getStats(magasinId);

        assertThat(result.revenuMoisCourant()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getStats_should_scope_caisse_filter_to_magasin() {
        stubAll(1L, 1L, BigDecimal.ZERO, 0L, BigDecimal.ZERO);

        service.getStats(magasinId);

        verify(caisseService).getResume(argThat(filter -> filter.magasinId().equals(magasinId)));
    }

    @Test
    void getStats_should_verify_magasin_ownership() {
        Magasin magasin = magasinWithEntreprise();
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureBelongsToCurrentEntreprise(magasin)).thenReturn(magasin);
        when(employeService.countByMagasinId(any())).thenReturn(0L);
        when(clientService.countByEntrepriseId(any())).thenReturn(0L);
        when(stockService.computeValuation(any(UUID.class))).thenReturn(new StockValuationResponse(magasinId, BigDecimal.ZERO, 0L));
        when(caisseService.getResume(any())).thenReturn(caisse(BigDecimal.ZERO));

        service.getStats(magasinId);

        verify(magasinService).ensureBelongsToCurrentEntreprise(magasin);
    }

    private static <T> T argThat(java.util.function.Predicate<T> predicate) {
        return org.mockito.ArgumentMatchers.argThat(predicate::test);
    }
}
