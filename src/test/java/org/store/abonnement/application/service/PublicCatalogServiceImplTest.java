package org.store.abonnement.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.dto.PublicCatalogResponse;
import org.store.abonnement.application.dto.PublicPlanResponse;
import org.store.abonnement.application.service.impl.PublicCatalogServiceImpl;
import org.store.abonnement.domain.service.PlanAbonnementDomainService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicCatalogServiceImplTest {

    @Mock
    private PlanAbonnementDomainService planAbonnementDomainService;

    @InjectMocks
    private PublicCatalogServiceImpl service;

    private PublicPlanResponse plan(UUID id, String nom, int ordre) {
        return new PublicPlanResponse(
                id, nom, null, new BigDecimal("9900"),
                1, 3,
                true, true, true, false,
                ordre);
    }

    @Test
    void findCatalog_should_call_findPublicResponses_and_return_plans() {
        UUID planAId = UUID.randomUUID();
        UUID planBId = UUID.randomUUID();

        when(planAbonnementDomainService.findPublicResponses())
                .thenReturn(List.of(plan(planAId, "Starter", 10), plan(planBId, "Pro", 20)));

        PublicCatalogResponse response = service.findCatalog();

        assertThat(response.plans()).hasSize(2);
        assertThat(response.plans().get(0).nom()).isEqualTo("Starter");
        assertThat(response.plans().get(1).nom()).isEqualTo("Pro");
        verify(planAbonnementDomainService).findPublicResponses();
    }

    @Test
    void findSubscribableCatalog_should_call_findSubscribableResponses_and_return_plans() {
        UUID planId = UUID.randomUUID();

        when(planAbonnementDomainService.findSubscribableResponses())
                .thenReturn(List.of(plan(planId, "Pro", 20)));

        PublicCatalogResponse response = service.findSubscribableCatalog();

        assertThat(response.plans()).hasSize(1);
        assertThat(response.plans().get(0).nom()).isEqualTo("Pro");
        verify(planAbonnementDomainService).findSubscribableResponses();
    }
}
