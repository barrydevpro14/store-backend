package org.store.reporting.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.reporting.application.dto.MagasinOverviewFilter;
import org.store.reporting.application.dto.MagasinOverviewStatsResponse;
import org.store.reporting.application.service.IMagasinReportingService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MagasinReportingControllerTest {

    private MockMvc mockMvc;
    private IMagasinReportingService magasinReportingService;

    private final UUID magasinId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        magasinReportingService = mock(IMagasinReportingService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new MagasinReportingController(magasinReportingService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .build();
    }

    private MagasinOverviewStatsResponse sampleResponse() {
        return new MagasinOverviewStatsResponse(
                10L,
                new BigDecimal("500.00"),
                new BigDecimal("480.00"),
                new BigDecimal("50.00"),
                new BigDecimal("12000.00"),
                2L,
                3L,
                4L
        );
    }

    @Test
    void should_return_200_with_all_kpis() throws Exception {
        when(magasinReportingService.getOverview(any(MagasinOverviewFilter.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(get(MagasinReportingController.BASE_PATH)
                        .param("magasinId", magasinId.toString())
                        .param("startDate", "2025-07-01")
                        .param("endDate", "2025-07-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCommandes").value(10))
                .andExpect(jsonPath("$.totalCommandes").value(500.00))
                .andExpect(jsonPath("$.totalPaiements").value(480.00))
                .andExpect(jsonPath("$.ticketMoyen").value(50.00))
                .andExpect(jsonPath("$.valeurStock").value(12000.00))
                .andExpect(jsonPath("$.produitsBasSeuil").value(2))
                .andExpect(jsonPath("$.achatsEnAttente").value(3))
                .andExpect(jsonPath("$.facturesImpayees").value(4));
    }

    @Test
    void should_return_400_when_magasin_id_missing() throws Exception {
        mockMvc.perform(get(MagasinReportingController.BASE_PATH)
                        .param("startDate", "2025-07-01")
                        .param("endDate", "2025-07-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_start_date_missing() throws Exception {
        mockMvc.perform(get(MagasinReportingController.BASE_PATH)
                        .param("magasinId", magasinId.toString())
                        .param("endDate", "2025-07-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_end_date_missing() throws Exception {
        mockMvc.perform(get(MagasinReportingController.BASE_PATH)
                        .param("magasinId", magasinId.toString())
                        .param("startDate", "2025-07-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
