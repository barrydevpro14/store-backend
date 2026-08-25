package org.store.plateforme.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.country.application.dto.CountryResponse;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.plateforme.application.dto.CategoryDepensePlateformeSummaryResponse;
import org.store.plateforme.application.dto.DepensePlateformeFilter;
import org.store.plateforme.application.dto.DepensePlateformeRequest;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.dto.DepensePlateformeTotalResponse;
import org.store.plateforme.application.service.IDepensePlateformeService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DepensePlateformeControllerTest {

    private MockMvc mockMvc;
    private IDepensePlateformeService service;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private UUID categoryId;
    private UUID moyenId;

    @BeforeEach
    void setUp() {
        service = mock(IDepensePlateformeService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new DepensePlateformeController(service))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();
        categoryId = UUID.randomUUID();
        moyenId = UUID.randomUUID();
    }

    @Test
    void should_return_201_when_depense_created() throws Exception {
        DepensePlateformeRequest body = new DepensePlateformeRequest(categoryId, "Serveur AWS", "desc",
                LocalDate.of(2026, 8, 1), new BigDecimal("500000.00"), moyenId, null, null);
        DepensePlateformeResponse sample = new DepensePlateformeResponse(
                UUID.randomUUID(),
                new CategoryDepensePlateformeSummaryResponse(categoryId, "Hébergement"),
                "Serveur AWS", "desc", "2026-08-01",
                new BigDecimal("500000.00"),
                new MoyenPaiementResponse(moyenId, "Virement", true),
                null,
                "2026-08-01 10:00:00",
                true);
        when(service.create(any(DepensePlateformeRequest.class))).thenReturn(sample);

        mockMvc.perform(post(DepensePlateformeController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.libelle").value("Serveur AWS"))
                .andExpect(jsonPath("$.actif").value(true));
    }

    @Test
    void should_return_200_with_total() throws Exception {
        when(service.computeTotal(any())).thenReturn(new DepensePlateformeTotalResponse(new BigDecimal("750000.00"), 3L));

        mockMvc.perform(get(DepensePlateformeController.BASE_PATH + "/total")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantTotal").value(750000.00))
                .andExpect(jsonPath("$.nombreDepenses").value(3));
    }

    @Test
    void list_should_accept_actif_query_param() throws Exception {
        Page<DepensePlateformeResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        ArgumentCaptor<DepensePlateformeFilter> captor = ArgumentCaptor.forClass(DepensePlateformeFilter.class);
        when(service.findAll(captor.capture())).thenReturn(page);

        mockMvc.perform(get(DepensePlateformeController.BASE_PATH)
                        .param("actif", "false"))
                .andExpect(status().isOk());

        assertThat(captor.getValue().actif()).isFalse();
    }
}
