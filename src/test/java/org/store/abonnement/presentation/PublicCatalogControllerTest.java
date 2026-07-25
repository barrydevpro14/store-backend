package org.store.abonnement.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.store.abonnement.application.dto.PublicCatalogResponse;
import org.store.abonnement.application.dto.PublicPlanResponse;
import org.store.abonnement.application.service.IPublicCatalogService;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;

import java.math.BigDecimal;
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

    private PublicPlanResponse plan(UUID id, String nom, int ordre) {
        return new PublicPlanResponse(
                id, nom, "Description", new BigDecimal("9900"),
                1, 3, true, true, true, false, ordre);
    }

    @Test
    void should_return_200_with_full_catalog() throws Exception {
        UUID planAId = UUID.randomUUID();
        UUID planBId = UUID.randomUUID();

        PublicCatalogResponse catalog = new PublicCatalogResponse(
                List.of(plan(planAId, "Starter", 10), plan(planBId, "Pro", 20)));

        when(publicCatalogService.findCatalog()).thenReturn(catalog);

        mockMvc.perform(get(PublicCatalogController.BASE_PATH + "/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans.length()").value(2))
                .andExpect(jsonPath("$.plans[0].nom").value("Starter"))
                .andExpect(jsonPath("$.plans[1].nom").value("Pro"));
    }

    @Test
    void should_return_200_with_empty_catalog_when_no_data() throws Exception {
        when(publicCatalogService.findCatalog())
                .thenReturn(new PublicCatalogResponse(List.of()));

        mockMvc.perform(get(PublicCatalogController.BASE_PATH + "/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plans.length()").value(0));
    }
}
