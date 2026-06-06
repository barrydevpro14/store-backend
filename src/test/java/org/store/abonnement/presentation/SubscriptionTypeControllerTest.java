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
import org.store.abonnement.application.dto.SubscriptionTypeFilter;
import org.store.abonnement.application.dto.SubscriptionTypeRequest;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.application.service.ISubscriptionTypeService;
import org.store.abonnement.domain.enums.ReductionType;
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

class SubscriptionTypeControllerTest {

    private MockMvc mockMvc;
    private ISubscriptionTypeService subscriptionTypeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID planId;
    private UUID typeId;
    private String basePath;

    @BeforeEach
    void setUp() {
        subscriptionTypeService = mock(ISubscriptionTypeService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new SubscriptionTypeController(subscriptionTypeService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        planId = UUID.randomUUID();
        typeId = UUID.randomUUID();
        basePath = "/api/v1/plans/" + planId + "/types";
    }

    private SubscriptionTypeRequest validBody() {
        return new SubscriptionTypeRequest(
                "Mensuel", 1, "POURCENTAGE", new BigDecimal("0"),
                false, true, 10);
    }

    private SubscriptionTypeResponse sample() {
        return new SubscriptionTypeResponse(typeId, planId, "Pro", "Mensuel", 1,
                ReductionType.POURCENTAGE, new BigDecimal("0"),
                false, true, 10);
    }

    @Test
    void should_return_201_when_created() throws Exception {
        when(subscriptionTypeService.create(eq(planId), any(SubscriptionTypeRequest.class)))
                .thenReturn(sample());

        mockMvc.perform(post(basePath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Mensuel"))
                .andExpect(jsonPath("$.planId").value(planId.toString()));
    }

    @Test
    void should_return_400_when_nom_blank() throws Exception {
        SubscriptionTypeRequest body = new SubscriptionTypeRequest(
                "", 1, "POURCENTAGE", new BigDecimal("0"), false, true, 0);

        mockMvc.perform(post(basePath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_invalid_reduction_type() throws Exception {
        SubscriptionTypeRequest body = new SubscriptionTypeRequest(
                "Mensuel", 1, "INVALID", new BigDecimal("0"), false, true, 0);

        mockMvc.perform(post(basePath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_page_when_list() throws Exception {
        Page<SubscriptionTypeResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(subscriptionTypeService.findAll(eq(planId), any(SubscriptionTypeFilter.class)))
                .thenReturn(page);

        mockMvc.perform(get(basePath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(subscriptionTypeService.findResponseById(eq(planId), eq(typeId))).thenReturn(sample());

        mockMvc.perform(get(basePath + "/" + typeId))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_200_when_updated() throws Exception {
        when(subscriptionTypeService.update(eq(planId), eq(typeId), any(SubscriptionTypeRequest.class)))
                .thenReturn(sample());

        mockMvc.perform(put(basePath + "/" + typeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_200_when_activated() throws Exception {
        when(subscriptionTypeService.activate(eq(planId), eq(typeId))).thenReturn(sample());

        mockMvc.perform(patch(basePath + "/" + typeId + "/activate"))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_204_when_deleted() throws Exception {
        mockMvc.perform(delete(basePath + "/" + typeId))
                .andExpect(status().isNoContent());

        verify(subscriptionTypeService).delete(planId, typeId);
    }
}
