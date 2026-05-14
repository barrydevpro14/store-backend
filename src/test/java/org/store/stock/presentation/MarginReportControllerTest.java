package org.store.stock.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.stock.application.dto.MarginReportFilter;
import org.store.stock.application.dto.MarginReportResponse;
import org.store.stock.application.service.IMarginReportService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarginReportControllerTest {

    private MockMvc mockMvc;
    private IMarginReportService marginReportService;

    private UUID magasinId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        marginReportService = mock(IMarginReportService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new MarginReportController(marginReportService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        magasinId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    @Test
    void should_return_200_with_margin_report() throws Exception {
        MarginReportResponse response = new MarginReportResponse(new BigDecimal("2750.00"), 150L, 2L);
        when(marginReportService.compute(any(MarginReportFilter.class))).thenReturn(response);

        mockMvc.perform(get(MarginReportController.BASE_PATH)
                        .param("magasinId", magasinId.toString())
                        .param("productId", productId.toString())
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.margeTotale").value(2750.00))
                .andExpect(jsonPath("$.quantiteVendueTotale").value(150))
                .andExpect(jsonPath("$.nombreSorties").value(2));

        verify(marginReportService).compute(eq(new MarginReportFilter(magasinId, productId, null, "2026-05-01", "2026-05-14")));
    }
}
