package org.store.abonnement.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.dto.PlanAbonnementSummaryResponse;
import org.store.abonnement.application.dto.PromotionResponse;
import org.store.abonnement.application.dto.PublicCatalogResponse;
import org.store.abonnement.application.dto.PublicPlanResponse;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.application.service.impl.PublicCatalogServiceImpl;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.abonnement.domain.service.PlanAbonnementDomainService;
import org.store.abonnement.domain.service.PromotionDomainService;
import org.store.abonnement.domain.service.TypePlanAbonnementDomainService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicCatalogServiceImplTest {

    @Mock private PlanAbonnementDomainService planAbonnementDomainService;
    @Mock private TypePlanAbonnementDomainService typePlanAbonnementDomainService;
    @Mock private PromotionDomainService promotionDomainService;

    @InjectMocks
    private PublicCatalogServiceImpl service;

    private UUID planAId;
    private UUID planBId;

    @BeforeEach
    void setUp() {
        planAId = UUID.randomUUID();
        planBId = UUID.randomUUID();
    }

    private PublicPlanResponse plan(UUID id, String nom, int ordre) {
        return new PublicPlanResponse(
                id, nom, null, new BigDecimal("9900"),
                1, 3,
                true, true, true, false,
                false, ordre);
    }

    private PromotionResponse promo(String nom, UUID planId, String planNom) {
        PlanAbonnementSummaryResponse summary = planId == null ? null
                : new PlanAbonnementSummaryResponse(planId, planNom, new BigDecimal("9900"));
        return new PromotionResponse(
                UUID.randomUUID(), nom, null,
                ReductionType.POURCENTAGE, new BigDecimal("15"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                true, summary);
    }

    private SubscriptionTypeResponse type(UUID planId, String planNom, String nom, int dureeMois) {
        return new SubscriptionTypeResponse(
                UUID.randomUUID(), planId, planNom, nom, dureeMois,
                null, null, dureeMois == 12, true, dureeMois);
    }

    @Test
    void findCatalog_should_aggregate_plans_types_and_promotions() {
        when(planAbonnementDomainService.findPublicResponses())
                .thenReturn(List.of(plan(planAId, "Starter", 10), plan(planBId, "Pro", 20)));
        when(typePlanAbonnementDomainService.findActifResponsesByPlanId(planAId))
                .thenReturn(List.of(type(planAId, "Starter", "Mensuel", 1)));
        when(typePlanAbonnementDomainService.findActifResponsesByPlanId(planBId))
                .thenReturn(List.of(type(planBId, "Pro", "Annuel", 12)));
        when(promotionDomainService.findActiveGlobalResponses(any(LocalDate.class)))
                .thenReturn(List.of(promo("Lancement global", null, null)));
        when(promotionDomainService.findActiveScopedResponses(any(LocalDate.class)))
                .thenReturn(List.of(promo("Black Friday Pro", planBId, "Pro")));

        PublicCatalogResponse response = service.findCatalog();

        assertThat(response.plans()).hasSize(2);
        assertThat(response.plans().get(0).subscriptionTypes())
                .extracting(SubscriptionTypeResponse::nom)
                .containsExactly("Mensuel");
        assertThat(response.plans().get(1).subscriptionTypes())
                .extracting(SubscriptionTypeResponse::nom)
                .containsExactly("Annuel");
        assertThat(response.globalPromotions())
                .extracting(PromotionResponse::nom)
                .containsExactly("Lancement global");
        assertThat(response.plans().get(0).promotions()).isEmpty();
        assertThat(response.plans().get(1).promotions())
                .extracting(PromotionResponse::nom)
                .containsExactly("Black Friday Pro");
    }

    @Test
    void findCatalog_should_return_empty_collections_when_no_data() {
        when(planAbonnementDomainService.findPublicResponses()).thenReturn(List.of());
        when(promotionDomainService.findActiveGlobalResponses(any(LocalDate.class))).thenReturn(List.of());
        when(promotionDomainService.findActiveScopedResponses(any(LocalDate.class))).thenReturn(List.of());

        PublicCatalogResponse response = service.findCatalog();

        assertThat(response.plans()).isEmpty();
        assertThat(response.globalPromotions()).isEmpty();
    }

    @Test
    void findCatalog_should_attach_multiple_promotions_to_same_plan() {
        when(planAbonnementDomainService.findPublicResponses())
                .thenReturn(List.of(plan(planAId, "Starter", 10)));
        when(typePlanAbonnementDomainService.findActifResponsesByPlanId(planAId)).thenReturn(List.of());
        when(promotionDomainService.findActiveGlobalResponses(any(LocalDate.class))).thenReturn(List.of());
        when(promotionDomainService.findActiveScopedResponses(any(LocalDate.class)))
                .thenReturn(List.of(
                        promo("Promo 1", planAId, "Starter"),
                        promo("Promo 2", planAId, "Starter")));

        PublicCatalogResponse response = service.findCatalog();

        assertThat(response.plans().get(0).promotions()).hasSize(2);
        assertThat(response.globalPromotions()).isEmpty();
    }
}
