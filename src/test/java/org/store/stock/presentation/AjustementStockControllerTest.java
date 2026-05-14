package org.store.stock.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.stock.application.dto.AjustementStockRequest;
import org.store.stock.application.dto.MouvementDetailResponse;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.stock.application.service.IAjustementStockService;
import org.store.stock.domain.enums.MotifAjustement;
import org.store.stock.domain.enums.MouvementStockType;
import org.store.stock.domain.enums.TypeAjustement;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AjustementStockControllerTest {

    private MockMvc mockMvc;
    private IAjustementStockService ajustementStockService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID magasinId;
    private UUID productId;
    private UUID productFournisseurId;

    @BeforeEach
    void setUp() {
        ajustementStockService = mock(IAjustementStockService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AjustementStockController(ajustementStockService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        magasinId = UUID.randomUUID();
        productId = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();
    }

    private AjustementStockRequest validPositif() {
        return new AjustementStockRequest(magasinId, productId, TypeAjustement.POSITIF, 20,
                productFournisseurId, new BigDecimal("10.00"), MotifAjustement.RETROUVAILLE, "retrouvé");
    }

    private AjustementStockRequest validNegatif() {
        return new AjustementStockRequest(magasinId, productId, TypeAjustement.NEGATIF, 5,
                null, null, MotifAjustement.CASSE, "casse");
    }

    private MouvementStockResponse sample() {
        return new MouvementStockResponse(
                UUID.randomUUID(), UUID.randomUUID(),
                new MagasinSummaryResponse(magasinId, "Magasin Central"),
                new ProductSummaryResponse(productId, "Clou 10mm", "CL-10"),
                new MouvementDetailResponse(MouvementStockType.AJUSTEMENT, 20, 100, 120, "RETROUVAILLE", "retrouvé"),
                "2026-05-14 10:00:00", UUID.randomUUID().toString()
        );
    }

    @Test
    void should_return_201_when_positif_adjustment_created() throws Exception {
        when(ajustementStockService.create(any(AjustementStockRequest.class))).thenReturn(sample());

        mockMvc.perform(post(AjustementStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPositif())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.detail.type").value("AJUSTEMENT"))
                .andExpect(jsonPath("$.detail.quantite").value(20))
                .andExpect(jsonPath("$.detail.referenceDocument").value("RETROUVAILLE"));
    }

    @Test
    void should_return_201_when_negatif_adjustment_created() throws Exception {
        when(ajustementStockService.create(any(AjustementStockRequest.class))).thenReturn(sample());

        mockMvc.perform(post(AjustementStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validNegatif())))
                .andExpect(status().isCreated());
    }

    @Test
    void should_return_400_when_required_fields_null() throws Exception {
        AjustementStockRequest body = new AjustementStockRequest(null, null, null, null,
                null, null, null, null);

        mockMvc.perform(post(AjustementStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_quantite_zero_or_negative() throws Exception {
        AjustementStockRequest body = new AjustementStockRequest(magasinId, productId, TypeAjustement.NEGATIF, 0,
                null, null, MotifAjustement.CASSE, null);

        mockMvc.perform(post(AjustementStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
