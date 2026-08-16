package org.store.abonnement.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.abonnement.application.dto.PlanAbonnementTarifRequest;
import org.store.abonnement.application.dto.PlanAbonnementTarifResponse;
import org.store.abonnement.application.service.IPlanAbonnementTarifService;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlanAbonnementTarifControllerTest {

    private MockMvc mockMvc;
    private IPlanAbonnementTarifService tarifService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID planId;
    private UUID tarifId;

    @BeforeEach
    void setUp() {
        tarifService = mock(IPlanAbonnementTarifService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new PlanAbonnementTarifController(tarifService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        planId = UUID.randomUUID();
        tarifId = UUID.randomUUID();
    }

    private PlanAbonnementTarifRequest validRequest() {
        return new PlanAbonnementTarifRequest(
                "MENSUEL", new BigDecimal("25000"), true, false, 1);
    }

    private PlanAbonnementTarifResponse sample() {
        return new PlanAbonnementTarifResponse(
                tarifId, PeriodiciteAbonnement.MENSUEL, new BigDecimal("25000"),
                true, false, 1, null, null, null);
    }

    private String path() {
        return "/api/v1/plans/" + planId + "/tarifs";
    }

    @Test
    void should_return_200_with_tarif_list() throws Exception {
        when(tarifService.findResponsesByPlan(eq(planId))).thenReturn(List.of(sample()));

        mockMvc.perform(get(path()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].periodicite").value("MENSUEL"))
                .andExpect(jsonPath("$[0].prix").value(25000));
    }

    @Test
    void should_return_201_when_tarif_created() throws Exception {
        when(tarifService.create(eq(planId), any(PlanAbonnementTarifRequest.class))).thenReturn(sample());

        mockMvc.perform(post(path())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.periodicite").value("MENSUEL"));
    }

    @Test
    void should_return_400_when_periodicite_invalid_enum() throws Exception {
        PlanAbonnementTarifRequest body = new PlanAbonnementTarifRequest(
                "HEBDOMADAIRE", new BigDecimal("25000"), true, false, null);

        mockMvc.perform(post(path())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_prix_zero() throws Exception {
        PlanAbonnementTarifRequest body = new PlanAbonnementTarifRequest(
                "MENSUEL", BigDecimal.ZERO, true, false, null);

        mockMvc.perform(post(path())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_prix_null() throws Exception {
        PlanAbonnementTarifRequest body = new PlanAbonnementTarifRequest(
                "MENSUEL", null, true, false, null);

        mockMvc.perform(post(path())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_when_tarif_updated() throws Exception {
        when(tarifService.update(eq(planId), eq(tarifId), any(PlanAbonnementTarifRequest.class)))
                .thenReturn(sample());

        mockMvc.perform(put(path() + "/" + tarifId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodicite").value("MENSUEL"));
    }

    @Test
    void should_return_400_when_update_request_invalid() throws Exception {
        PlanAbonnementTarifRequest body = new PlanAbonnementTarifRequest(
                "MENSUEL", BigDecimal.ZERO, true, false, null);

        mockMvc.perform(put(path() + "/" + tarifId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_204_when_tarif_deleted() throws Exception {
        mockMvc.perform(delete(path() + "/" + tarifId))
                .andExpect(status().isNoContent());

        verify(tarifService).delete(planId, tarifId);
    }
}
