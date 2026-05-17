package org.store.abonnement.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.store.abonnement.application.dto.PlanAbonnementSummaryResponse;
import org.store.abonnement.application.dto.PromotionResponse;
import org.store.abonnement.application.dto.PublicCatalogResponse;
import org.store.abonnement.application.dto.PublicPlanResponse;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.application.service.IPublicCatalogService;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicCatalogControllerTest {

    private MockMvc mockMvc;
    private IPublicCatalogService publicCatalogService;

    @BeforeEach
    void setUp() {
        publicCatalogService = mock(IPublicCatalogService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        mockMvc = MockMvcBuilders.standaloneSetup(new PublicCatalogController(publicCatalogService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .build();
    }

    @Test
    void should_return_200_with_full_catalog() throws Exception {
        UUID planAId = UUID.randomUUID();
        UUID planBId = UUID.randomUUID();

        PromotionResponse promoPlanB = new PromotionResponse(
                UUID.randomUUID(), "Black Friday", null,
                ReductionType.POURCENTAGE, new BigDecimal("15"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                true,
                new PlanAbonnementSummaryResponse(planBId, "Pro", new BigDecimal("19900")));

        PromotionResponse globalPromo = new PromotionResponse(
                UUID.randomUUID(), "Lancement", null,
                ReductionType.POURCENTAGE, new BigDecimal("10"),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                true, null);

        PublicPlanResponse planA = new PublicPlanResponse(
                planAId, "Starter", "Pour un magasin", new BigDecimal("9900"),
                1, 3, true, true, true, false, false, 10, List.of());
        PublicPlanResponse planB = new PublicPlanResponse(
                planBId, "Pro", "Pour 5 magasins", new BigDecimal("19900"),
                5, 20, true, true, true, true, false, 20, List.of(promoPlanB));

        SubscriptionTypeResponse mensuel = new SubscriptionTypeResponse(
                UUID.randomUUID(), "Mensuel", 1, null, null, false, true, 1);
        SubscriptionTypeResponse annuel = new SubscriptionTypeResponse(
                UUID.randomUUID(), "Annuel", 12,
                ReductionType.POURCENTAGE, new BigDecimal("15"), true, true, 12);

        PublicCatalogResponse catalog = new PublicCatalogResponse(
                List.of(planA, planB),
                List.of(mensuel, annuel),
                List.of(globalPromo));

        when(publicCatalogService.findCatalog()).thenReturn(catalog);

        mockMvc.perform(get(PublicCatalogController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans.length()").value(2))
                .andExpect(jsonPath("$.plans[0].nom").value("Starter"))
                .andExpect(jsonPath("$.plans[1].nom").value("Pro"))
                .andExpect(jsonPath("$.plans[1].promotions.length()").value(1))
                .andExpect(jsonPath("$.plans[1].promotions[0].nom").value("Black Friday"))
                .andExpect(jsonPath("$.subscriptionTypes.length()").value(2))
                .andExpect(jsonPath("$.subscriptionTypes[1].nom").value("Annuel"))
                .andExpect(jsonPath("$.globalPromotions.length()").value(1))
                .andExpect(jsonPath("$.globalPromotions[0].nom").value("Lancement"));
    }

    @Test
    void should_return_200_with_empty_catalog_when_no_data() throws Exception {
        when(publicCatalogService.findCatalog())
                .thenReturn(new PublicCatalogResponse(List.of(), List.of(), List.of()));

        mockMvc.perform(get(PublicCatalogController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans.length()").value(0))
                .andExpect(jsonPath("$.subscriptionTypes.length()").value(0))
                .andExpect(jsonPath("$.globalPromotions.length()").value(0));
    }
}
