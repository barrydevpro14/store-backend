package org.store.abonnement.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.abonnement.application.dto.PromotionFilter;
import org.store.abonnement.application.dto.PromotionRequest;
import org.store.abonnement.application.dto.PromotionResponse;
import org.store.abonnement.application.service.IPromotionService;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;

import java.math.BigDecimal;
import java.time.LocalDate;
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

class PromotionControllerTest {

    private MockMvc mockMvc;
    private IPromotionService promotionService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID promotionId;

    @BeforeEach
    void setUp() {
        promotionService = mock(IPromotionService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new PromotionController(promotionService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        promotionId = UUID.randomUUID();
    }

    private PromotionRequest validBody() {
        return new PromotionRequest(
                "Rentrée 2026", null, "POURCENTAGE", new BigDecimal("15"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                true, null);
    }

    private PromotionResponse sample() {
        return new PromotionResponse(promotionId, "Rentrée 2026", null,
                ReductionType.POURCENTAGE, new BigDecimal("15"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                true, null);
    }

    @Test
    void should_return_201_when_created() throws Exception {
        when(promotionService.create(any(PromotionRequest.class))).thenReturn(sample());

        mockMvc.perform(post(PromotionController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Rentrée 2026"));
    }

    @Test
    void should_return_400_when_nom_blank() throws Exception {
        PromotionRequest body = new PromotionRequest(
                "", null, "POURCENTAGE", new BigDecimal("10"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                true, null);

        mockMvc.perform(post(PromotionController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_page_when_list() throws Exception {
        Page<PromotionResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(promotionService.findAll(any(PromotionFilter.class))).thenReturn(page);

        mockMvc.perform(get(PromotionController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(promotionService.findResponseById(eq(promotionId))).thenReturn(sample());

        mockMvc.perform(get(PromotionController.BASE_PATH + "/" + promotionId))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_200_when_updated() throws Exception {
        when(promotionService.update(eq(promotionId), any(PromotionRequest.class))).thenReturn(sample());

        mockMvc.perform(put(PromotionController.BASE_PATH + "/" + promotionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_200_when_activated() throws Exception {
        when(promotionService.activate(eq(promotionId))).thenReturn(sample());

        mockMvc.perform(patch(PromotionController.BASE_PATH + "/" + promotionId + "/activate"))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_204_when_deleted() throws Exception {
        mockMvc.perform(delete(PromotionController.BASE_PATH + "/" + promotionId))
                .andExpect(status().isNoContent());

        verify(promotionService).delete(promotionId);
    }
}
