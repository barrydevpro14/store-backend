package org.store.paiement.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.country.application.dto.CountryResponse;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.application.dto.FacturationResponse;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.paiement.application.service.IFacturationService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FacturationControllerTest {

    private MockMvc mockMvc;
    private IFacturationService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = mock(IFacturationService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new FacturationController(service))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();
    }

    @Test
    void create_should_return_201() throws Exception {
        FacturationRequest body = new FacturationRequest(UUID.randomUUID(), null, "77 000 00 00");
        MoyenPaiementResponse moyenPaiement = new MoyenPaiementResponse(UUID.randomUUID(), "Wave", true, List.of());
        FacturationResponse response = new FacturationResponse(UUID.randomUUID(), moyenPaiement, (CountryResponse) null, "77 000 00 00", true);
        when(service.create(any(FacturationRequest.class))).thenReturn(response);

        mockMvc.perform(post(FacturationController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroFacturation").value("77 000 00 00"));
    }

    @Test
    void create_should_return_400_when_numeroFacturation_blank() throws Exception {
        FacturationRequest body = new FacturationRequest(UUID.randomUUID(), null, "");

        mockMvc.perform(post(FacturationController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_should_return_400_when_moyenPaiementId_missing() throws Exception {
        String bodyWithoutMoyen = """
                {"paysId": null, "numeroFacturation": "77 000 00 00"}
                """;

        mockMvc.perform(post(FacturationController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithoutMoyen))
                .andExpect(status().isBadRequest());
    }
}
