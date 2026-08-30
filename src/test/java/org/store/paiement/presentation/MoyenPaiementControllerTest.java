package org.store.paiement.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.dto.DataSelect;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.paiement.application.dto.MoyenPaiementRequest;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.paiement.application.dto.MoyenPaiementSelectFilter;
import org.store.paiement.application.service.IMoyenPaiementService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MoyenPaiementControllerTest {

    private MockMvc mockMvc;
    private IMoyenPaiementService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = mock(IMoyenPaiementService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new MoyenPaiementController(service))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();
    }

    @Test
    void select_should_pass_query_params_to_service() throws Exception {
        UUID countryId = UUID.randomUUID();
        DataSelect item = new DataSelect(UUID.randomUUID().toString(), "Wave");
        when(service.findSelectItems(new MoyenPaiementSelectFilter(countryId, "wa", 0, 10)))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1));

        mockMvc.perform(get(MoyenPaiementController.BASE_PATH + "/select")
                        .param("countryId", countryId.toString())
                        .param("q", "wa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].label").value("Wave"));
    }

    @Test
    void select_should_work_without_countryId() throws Exception {
        when(service.findSelectItems(new MoyenPaiementSelectFilter(null, null, 0, 10)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get(MoyenPaiementController.BASE_PATH + "/select"))
                .andExpect(status().isOk());
    }

    @Test
    void create_should_return_201_with_paysIds() throws Exception {
        MoyenPaiementRequest body = new MoyenPaiementRequest("Wave", Set.of());
        when(service.create(any(MoyenPaiementRequest.class)))
                .thenReturn(new MoyenPaiementResponse(UUID.randomUUID(), "Wave", true, List.of()));

        mockMvc.perform(post(MoyenPaiementController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.libelle").value("Wave"));
    }
}
