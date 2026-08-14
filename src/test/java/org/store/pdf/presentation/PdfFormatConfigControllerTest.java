package org.store.pdf.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.pdf.application.dto.PdfFormatConfigResponse;
import org.store.pdf.application.service.IPdfFormatConfigService;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PdfFormatConfigControllerTest {

    private MockMvc mockMvc;
    private IPdfFormatConfigService service;

    private UUID configA4Id;
    private UUID configA5Id;

    @BeforeEach
    void setUp() {
        service = mock(IPdfFormatConfigService.class);
        IMessageSourceService messageSource = mock(IMessageSourceService.class);

        mockMvc = MockMvcBuilders.standaloneSetup(new PdfFormatConfigController(service))
                .setControllerAdvice(new GlobalException(messageSource))
                .build();

        configA4Id = UUID.randomUUID();
        configA5Id = UUID.randomUUID();
    }

    @Test
    void list_should_return_200_with_all_enabled_configs() throws Exception {
        List<PdfFormatConfigResponse> configs = List.of(
                new PdfFormatConfigResponse(configA4Id, "A4"),
                new PdfFormatConfigResponse(configA5Id, "A5")
        );
        when(service.findAll()).thenReturn(configs);

        mockMvc.perform(get(PdfFormatConfigController.BASE_PATH).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(configA4Id.toString()))
                .andExpect(jsonPath("$[0].label").value("A4"))
                .andExpect(jsonPath("$[1].id").value(configA5Id.toString()))
                .andExpect(jsonPath("$[1].label").value("A5"));
    }

    @Test
    void list_should_return_empty_array_when_no_config_enabled() throws Exception {
        when(service.findAll()).thenReturn(List.of());

        mockMvc.perform(get(PdfFormatConfigController.BASE_PATH).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
