package org.store.abonnement.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.dto.PublicCatalogResponse;
import org.store.abonnement.application.service.impl.PublicCatalogServiceImpl;
import org.store.abonnement.application.service.impl.SubscriptionAmountCalculator;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;
import org.store.abonnement.domain.model.TarifAvecCoupon;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicCatalogServiceImplTest {

    @Mock
    private IPlanAbonnementService planService;

    @Mock
    private IPlanAbonnementTarifService tarifService;

    @Mock
    private SubscriptionAmountCalculator amountCalculator;

    @InjectMocks
    private PublicCatalogServiceImpl service;

    private PlanAbonnement plan(String nom, int ordre) {
        PlanAbonnement plan = new PlanAbonnement();
        plan.setId(UUID.randomUUID());
        plan.setNom(nom);
        plan.setOrdre(ordre);
        return plan;
    }

    @Test
    void findCatalog_should_call_findPublicPlans_and_return_plans() {
        when(planService.findPublicPlans())
                .thenReturn(List.of(plan("Starter", 10), plan("Pro", 20)));
        when(tarifService.findActifWithCoupon(any())).thenReturn(List.of());

        PublicCatalogResponse response = service.findCatalog();

        assertThat(response.plans()).hasSize(2);
        assertThat(response.plans().get(0).nom()).isEqualTo("Starter");
        assertThat(response.plans().get(1).nom()).isEqualTo("Pro");
        verify(planService).findPublicPlans();
    }

    @Test
    void findSubscribableCatalog_should_call_findSubscribablePlans_and_return_plans() {
        when(planService.findSubscribablePlans())
                .thenReturn(List.of(plan("Pro", 20)));
        when(tarifService.findActifWithCoupon(any())).thenReturn(List.of());

        PublicCatalogResponse response = service.findSubscribableCatalog();

        assertThat(response.plans()).hasSize(1);
        assertThat(response.plans().get(0).nom()).isEqualTo("Pro");
        verify(planService).findSubscribablePlans();
    }

    @Test
    void findCatalog_should_return_plain_tarif_response_when_coupon_is_null() {
        PlanAbonnement plan = plan("Basic", 1);
        TarifAvecCoupon tacSansCoupon = new TarifAvecCoupon(new PlanAbonnementTarif(), null);

        when(planService.findPublicPlans()).thenReturn(List.of(plan));
        when(tarifService.findActifWithCoupon(plan.getId())).thenReturn(List.of(tacSansCoupon));

        PublicCatalogResponse response = service.findCatalog();

        assertThat(response.plans()).hasSize(1);
        assertThat(response.plans().get(0).tarifs()).hasSize(1);
        assertThat(response.plans().get(0).tarifs().get(0).reductionPourcentage()).isNull();
    }
}
