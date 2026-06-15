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
import org.store.stock.application.dto.MouvementDetailResponse;
import org.store.stock.application.dto.MouvementStockFilter;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.stock.application.service.IMouvementStockService;
import org.store.stock.domain.enums.MouvementStockType;


import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MouvementStockControllerTest {

    private MockMvc mockMvc;
    private IMouvementStockService mouvementStockService;

    private UUID mouvementId;
    private UUID stockId;
    private UUID magasinId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        mouvementStockService = mock(IMouvementStockService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new MouvementStockController(mouvementStockService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        mouvementId = UUID.randomUUID();
        stockId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    private MouvementStockResponse sample() {
        return new MouvementStockResponse(
                mouvementId, stockId,
                new MagasinSummaryResponse(magasinId, "Magasin Central"),
                new ProductSummaryResponse(productId, "Clou 10mm", "CL-10", null),
                new MouvementDetailResponse(MouvementStockType.ENTREE_ACHAT, 100, 0, 100, "LOT-001", "achat manuel"),
                "2026-05-14 10:00:00", UUID.randomUUID().toString()
        );
    }

    @Test
    void should_return_200_with_default_pagination_when_only_magasinId() throws Exception {
        Page<MouvementStockResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(mouvementStockService.findAllByCurrentEntreprise(any(MouvementStockFilter.class))).thenReturn(page);

        mockMvc.perform(get(MouvementStockController.BASE_PATH).param("magasinId", magasinId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(mouvementId.toString()))
                .andExpect(jsonPath("$.content[0].detail.type").value("ENTREE_ACHAT"))
                .andExpect(jsonPath("$.content[0].detail.quantite").value(100));

        verify(mouvementStockService).findAllByCurrentEntreprise(eq(new MouvementStockFilter(magasinId, null, null, null, null, null, 0, 10)));
    }

    @Test
    void should_return_200_when_filter_with_all_params() throws Exception {
        Page<MouvementStockResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(1, 5), 1);
        when(mouvementStockService.findAllByCurrentEntreprise(any(MouvementStockFilter.class))).thenReturn(page);

        mockMvc.perform(get(MouvementStockController.BASE_PATH)
                        .param("magasinId", magasinId.toString())
                        .param("productId", productId.toString())
                        .param("stockId", stockId.toString())
                        .param("type", "ENTREE_ACHAT")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-14")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(mouvementStockService).findAllByCurrentEntreprise(eq(new MouvementStockFilter(magasinId, productId, stockId, "ENTREE_ACHAT", "2026-05-01", "2026-05-14", 1, 5)));
    }
}
