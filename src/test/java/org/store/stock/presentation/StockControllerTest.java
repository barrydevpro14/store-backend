package org.store.stock.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.application.service.IStockService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockControllerTest {

    private MockMvc mockMvc;
    private IStockService stockService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID stockId;
    private UUID magasinId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        stockService = mock(IStockService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new StockController(stockService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
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
                .andExpect(jsonPath("$.id").value(stockId.toString()))
                .andExpect(jsonPath("$.magasin.id").value(magasinId.toString()))
                .andExpect(jsonPath("$.produit.id").value(productId.toString()))
                .andExpect(jsonPath("$.quantiteDisponible").value(150))
                .andExpect(jsonPath("$.seuilApprovisionnement").value(20))
                .andExpect(jsonPath("$.prixAchatMoyen").value(13.33));
    }

    @Test
    void should_return_200_with_page_when_list_no_filter() throws Exception {
        Page<StockResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(stockService.findAllByCurrentEntreprise(isNull(), isNull(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(StockController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(stockId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_with_page_when_filter_by_magasin() throws Exception {
        Page<StockResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(stockService.findAllByCurrentEntreprise(eq(magasinId), isNull(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(StockController.BASE_PATH).param("magasinId", magasinId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].magasin.id").value(magasinId.toString()));

        verify(stockService).findAllByCurrentEntreprise(eq(magasinId), isNull(), any(Pageable.class));
    }

    @Test
    void should_return_200_with_page_when_filter_by_product() throws Exception {
        Page<StockResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(stockService.findAllByCurrentEntreprise(isNull(), eq(productId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(StockController.BASE_PATH).param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].produit.id").value(productId.toString()));

        verify(stockService).findAllByCurrentEntreprise(isNull(), eq(productId), any(Pageable.class));
    }

    @Test
    void should_return_200_with_page_when_filter_by_both() throws Exception {
        Page<StockResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(stockService.findAllByCurrentEntreprise(eq(magasinId), eq(productId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(StockController.BASE_PATH)
                        .param("magasinId", magasinId.toString())
                        .param("productId", productId.toString()))
                .andExpect(status().isOk());

        verify(stockService).findAllByCurrentEntreprise(eq(magasinId), eq(productId), any(Pageable.class));
    }
}
