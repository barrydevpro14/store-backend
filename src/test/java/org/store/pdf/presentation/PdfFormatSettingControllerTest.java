package org.store.pdf.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.pdf.application.dto.PdfFormatSettingRequest;
import org.store.pdf.application.dto.PdfFormatSettingResponse;
import org.store.pdf.application.service.IPdfFormatSettingService;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PdfFormatSettingControllerTest {

    private MockMvc mockMvc;
    private IPdfFormatSettingService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID pdfFormatConfigId;
    private UUID magasinId;

    @BeforeEach
    void setUp() {
        service = mock(IPdfFormatSettingService.class);
        IMessageSourceService messageSource = mock(IMessageSourceService.class);

        mockMvc = MockMvcBuilders.standaloneSetup(new PdfFormatSettingController(service))
                .setControllerAdvice(new GlobalException(messageSource))
                .build();

        pdfFormatConfigId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
    }

    @Test
    void list_should_return_200_with_effective_settings() throws Exception {
        PdfFormatSettingResponse response = new PdfFormatSettingResponse(
                pdfFormatConfigId, "THERMAL_58MM", "Ticket thermique 58mm",
                BigDecimal.valueOf(136), null,
                BigDecimal.valueOf(4), BigDecimal.valueOf(4), BigDecimal.valueOf(4), BigDecimal.valueOf(4),
                BigDecimal.valueOf(10), BigDecimal.valueOf(8), BigDecimal.valueOf(7), false);
        when(service.findEffectiveForMagasin(magasinId)).thenReturn(List.of(response));

        mockMvc.perform(get(PdfFormatSettingController.BASE_PATH).param("magasinId", magasinId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("THERMAL_58MM"))
                .andExpect(jsonPath("$[0].overridden").value(false));
    }

    @Test
    void upsertOverride_should_return_200_with_updated_setting() throws Exception {
        PdfFormatSettingRequest request = new PdfFormatSettingRequest(
                BigDecimal.valueOf(204), null,
                BigDecimal.valueOf(5), BigDecimal.valueOf(5), BigDecimal.valueOf(5), BigDecimal.valueOf(5),
                BigDecimal.valueOf(10), BigDecimal.valueOf(8), BigDecimal.valueOf(7));
        PdfFormatSettingResponse response = new PdfFormatSettingResponse(
                pdfFormatConfigId, "THERMAL_58MM", "Ticket thermique 58mm",
                BigDecimal.valueOf(204), null,
                BigDecimal.valueOf(5), BigDecimal.valueOf(5), BigDecimal.valueOf(5), BigDecimal.valueOf(5),
                BigDecimal.valueOf(10), BigDecimal.valueOf(8), BigDecimal.valueOf(7), true);
        when(service.upsertOverride(eq(pdfFormatConfigId), eq(magasinId), any())).thenReturn(response);

        mockMvc.perform(put(PdfFormatSettingController.BASE_PATH + "/" + pdfFormatConfigId)
                        .param("magasinId", magasinId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overridden").value(true))
                .andExpect(jsonPath("$.pageWidth").value(204));
    }

    @Test
    void deleteOverride_should_return_204() throws Exception {
        mockMvc.perform(delete(PdfFormatSettingController.BASE_PATH + "/" + pdfFormatConfigId)
                        .param("magasinId", magasinId.toString()))
                .andExpect(status().isNoContent());

        verify(service).deleteOverride(pdfFormatConfigId, magasinId);
    }
}
