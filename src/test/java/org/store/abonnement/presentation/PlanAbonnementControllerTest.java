package org.store.abonnement.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.abonnement.application.dto.PlanAbonnementFilter;
import org.store.abonnement.application.dto.PlanAbonnementRequest;
import org.store.abonnement.application.dto.PlanAbonnementResponse;
import org.store.abonnement.application.service.IPlanAbonnementService;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlanAbonnementControllerTest {

    private MockMvc mockMvc;
    private IPlanAbonnementService planAbonnementService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID planId;

    @BeforeEach
    void setUp() {
        planAbonnementService = mock(IPlanAbonnementService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new PlanAbonnementController(planAbonnementService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        planId = UUID.randomUUID();
    }

    private PlanAbonnementRequest validBody() {
        return new PlanAbonnementRequest(
                "Starter", "Plan d'entrée de gamme",
                new BigDecimal("9900"), 1, 3,
                true, true, true, false, true, true, false, 10);
    }

    private PlanAbonnementResponse sample() {
        return new PlanAbonnementResponse(planId, "Starter", "Plan d'entrée de gamme",
                new BigDecimal("9900"), 1, 3,
                true, true, true, false, true, true, 10);
    }

    @Test
    void should_return_201_when_created() throws Exception {
        when(planAbonnementService.create(any(PlanAbonnementRequest.class))).thenReturn(sample());

        mockMvc.perform(post(PlanAbonnementController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(planId.toString()))
                .andExpect(jsonPath("$.nom").value("Starter"));
    }

    @Test
    void should_return_400_when_nom_blank() throws Exception {
        PlanAbonnementRequest body = new PlanAbonnementRequest(
                "", null, new BigDecimal("100"), 1, 1,
                true, true, true, false, true, true, false, 0);

        mockMvc.perform(post(PlanAbonnementController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_prix_null() throws Exception {
        String json = """
                { "nom":"X", "prix":null, "nombreMagasinsMax":1, "nombreEmployesMax":1,
                  "gestionStock":true, "gestionVente":true, "gestionAchat":true, "gestionComptabilite":false,
                  "actif":true, "visible":true, "trial":false, "ordre":0 }
                """;

        mockMvc.perform(post(PlanAbonnementController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_page_when_list() throws Exception {
        Page<PlanAbonnementResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(planAbonnementService.findAll(any(PlanAbonnementFilter.class))).thenReturn(page);

        mockMvc.perform(get(PlanAbonnementController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(planId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(planAbonnementService.findResponseById(eq(planId))).thenReturn(sample());

        mockMvc.perform(get(PlanAbonnementController.BASE_PATH + "/" + planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Starter"));
    }

    @Test
    void should_return_200_when_updated() throws Exception {
        when(planAbonnementService.update(eq(planId), any(PlanAbonnementRequest.class))).thenReturn(sample());

        mockMvc.perform(put(PlanAbonnementController.BASE_PATH + "/" + planId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_200_when_activated() throws Exception {
        when(planAbonnementService.activate(eq(planId))).thenReturn(sample());

        mockMvc.perform(patch(PlanAbonnementController.BASE_PATH + "/" + planId + "/activate"))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_200_when_deactivated() throws Exception {
        when(planAbonnementService.deactivate(eq(planId))).thenReturn(sample());

        mockMvc.perform(patch(PlanAbonnementController.BASE_PATH + "/" + planId + "/deactivate"))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_204_when_deleted() throws Exception {
        mockMvc.perform(delete(PlanAbonnementController.BASE_PATH + "/" + planId))
                .andExpect(status().isNoContent());

        verify(planAbonnementService).delete(planId);
    }
}
