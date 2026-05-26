package org.store.stock.presentation;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.stock.application.dto.ExpiringLotResponse;
import org.store.stock.application.dto.ExpiringLotsFilter;
import org.store.stock.application.dto.StockFilter;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.application.dto.StockThresholdRequest;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.application.service.IExpiringLotsService;
import org.store.stock.application.service.IStockService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockControllerTest {

    private MockMvc mockMvc;
    private IStockService stockService;
    private IExpiringLotsService expiringLotsService;

    private UUID stockId;
    private UUID magasinId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        stockService = mock(IStockService.class);
        expiringLotsService = mock(IExpiringLotsService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new StockController(stockService, expiringLotsService), new org.store.reporting.presentation.StockValuationController(stockService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        stockId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    private StockResponse sample() {
        return new StockResponse(
                stockId,
                new MagasinSummaryResponse(magasinId, "Magasin Central"),
                new ProductSummaryResponse(productId, "Clou 10mm", "CL-10"),
                150, 20,
                new BigDecimal("13.33"),
                null, null
        );
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(stockService.findResponseById(eq(stockId))).thenReturn(sample());

        mockMvc.perform(get(StockController.BASE_PATH + "/" + stockId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(stockId.toString()));
    }

    @Test
    void should_return_200_with_default_pagination_when_only_magasinId() throws Exception {
        Page<StockResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(stockService.findAllByCurrentEntreprise(any(StockFilter.class))).thenReturn(page);

        mockMvc.perform(get(StockController.BASE_PATH).param("magasinId", magasinId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(stockId.toString()));

        verify(stockService).findAllByCurrentEntreprise(eq(new StockFilter(magasinId, null, 0, 10)));
    }

    @Test
    void should_return_200_when_filter_with_pagination() throws Exception {
        Page<StockResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(1, 5), 1);
        when(stockService.findAllByCurrentEntreprise(any(StockFilter.class))).thenReturn(page);

        mockMvc.perform(get(StockController.BASE_PATH)
                        .param("magasinId", magasinId.toString())
                        .param("productId", productId.toString())
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(stockService).findAllByCurrentEntreprise(eq(new StockFilter(magasinId, productId, 1, 5)));
    }

    @Test
    void should_return_200_with_page_when_list_below_threshold() throws Exception {
        Page<StockResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(stockService.findBelowThresholdByCurrentEntreprise(any(StockFilter.class))).thenReturn(page);

        mockMvc.perform(get(StockController.BASE_PATH + "/below-threshold")
                        .param("magasinId", magasinId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(stockId.toString()));

        verify(stockService).findBelowThresholdByCurrentEntreprise(eq(new StockFilter(magasinId, null, 0, 10)));
    }

    @Test
    void should_return_200_when_threshold_updated() throws Exception {
        when(stockService.updateThreshold(eq(stockId), any(StockThresholdRequest.class))).thenReturn(sample());

        mockMvc.perform(patch(StockController.BASE_PATH + "/" + stockId + "/threshold")
                        .contentType(APPLICATION_JSON)
                        .content("{\"seuilApprovisionnement\": 30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(stockId.toString()));
    }

    @Test
    void should_return_400_when_threshold_negative() throws Exception {
        mockMvc.perform(patch(StockController.BASE_PATH + "/" + stockId + "/threshold")
                        .contentType(APPLICATION_JSON)
                        .content("{\"seuilApprovisionnement\": -1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_valuation() throws Exception {
        StockValuationResponse response = new StockValuationResponse(magasinId, new BigDecimal("12345.67"), 5L);
        when(stockService.computeValuation(eq(magasinId))).thenReturn(response);

        mockMvc.perform(get(StockController.BASE_PATH + "/valuation")
                        .param("magasinId", magasinId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.magasinId").value(magasinId.toString()))
                .andExpect(jsonPath("$.valeurTotale").value(12345.67))
                .andExpect(jsonPath("$.nombreLignes").value(5));
    }

    @Test
    void should_return_200_with_expiring_lots() throws Exception {
        Page<ExpiringLotResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(expiringLotsService.findExpiringLots(any(ExpiringLotsFilter.class))).thenReturn(page);

        mockMvc.perform(get(StockController.BASE_PATH + "/expiring-lots")
                        .param("magasinId", magasinId.toString())
                        .param("daysAhead", "60"))
                .andExpect(status().isOk());

        verify(expiringLotsService).findExpiringLots(eq(new ExpiringLotsFilter(magasinId, null, 60, 0, 10)));
    }
}
