package org.store.plateforme.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.store.plateforme.application.dto.PlateformePeriodReportResponse;
import org.store.plateforme.application.service.IPlateformeReportingService;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlateformeReportingControllerTest {

    private MockMvc mockMvc;
    private IPlateformeReportingService service;

    @BeforeEach
    void setUp() {
        service = mock(IPlateformeReportingService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PlateformeReportingController(service)).build();
    }

    @Test
    void should_return_200_with_period_report() throws Exception {
        when(service.getPeriodReport(any()))
                .thenReturn(new PlateformePeriodReportResponse(new BigDecimal("1000000.00"), new BigDecimal("300000.00"), new BigDecimal("700000.00")));

        mockMvc.perform(get(PlateformeReportingController.BASE_PATH + "/period")
                        .param("startDate", "2026-08-01")
                        .param("endDate", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenu").value(1000000.00))
                .andExpect(jsonPath("$.benefice").value(700000.00));
    }
}
