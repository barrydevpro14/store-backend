package org.store.stock.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.achat.application.dto.FournisseurSummaryResponse;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.application.dto.EntreeStockResponse;
import org.store.stock.application.service.IEntreeStockService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EntreeStockControllerTest {

    private MockMvc mockMvc;
    private IEntreeStockService entreeStockService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID entreeStockId;
    private UUID magasinId;
    private UUID productId;
    private UUID productFournisseurId;
    private UUID fournisseurId;

    @BeforeEach
    void setUp() {
        entreeStockService = mock(IEntreeStockService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new EntreeStockController(entreeStockService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        entreeStockId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
        productId = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();
        fournisseurId = UUID.randomUUID();
    }

    private EntreeStockResponse sample() {
        return new EntreeStockResponse(
                entreeStockId,
                new MagasinSummaryResponse(magasinId, "Magasin Central"),
                new ProductSummaryResponse(productId, "Clou 10mm", "CL-10"),
                new FournisseurSummaryResponse(fournisseurId, "Fournisseur Chine"),
                100, 100,
                new BigDecimal("10.00"), "LOT-001",
                "2027-05-14",
                "2026-05-14 10:00:00"
        );
    }

    private EntreeStockRequest validBody() {
        return new EntreeStockRequest(magasinId, productFournisseurId, 100, new BigDecimal("10.00"), "LOT-001", LocalDate.now().plusYears(1), "achat manuel");
    }

    @Test
    void should_return_201_when_created() throws Exception {
        when(entreeStockService.create(any(EntreeStockRequest.class))).thenReturn(sample());

        mockMvc.perform(post(EntreeStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(entreeStockId.toString()))
                .andExpect(jsonPath("$.magasin.id").value(magasinId.toString()))
                .andExpect(jsonPath("$.produit.id").value(productId.toString()))
                .andExpect(jsonPath("$.fournisseur.id").value(fournisseurId.toString()))
                .andExpect(jsonPath("$.quantiteInitiale").value(100))
                .andExpect(jsonPath("$.quantiteRestante").value(100))
                .andExpect(jsonPath("$.prixAchat").value(10.00))
                .andExpect(jsonPath("$.numeroLot").value("LOT-001"));
    }

    @Test
    void should_return_400_when_magasinId_null() throws Exception {
        EntreeStockRequest body = new EntreeStockRequest(null, productFournisseurId, 100, new BigDecimal("10.00"), null, null, null);

        mockMvc.perform(post(EntreeStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_productFournisseurId_null() throws Exception {
        EntreeStockRequest body = new EntreeStockRequest(magasinId, null, 100, new BigDecimal("10.00"), null, null, null);

        mockMvc.perform(post(EntreeStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_quantite_zero_or_negative() throws Exception {
        EntreeStockRequest body = new EntreeStockRequest(magasinId, productFournisseurId, 0, new BigDecimal("10.00"), null, null, null);

        mockMvc.perform(post(EntreeStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_prixAchat_zero_or_negative() throws Exception {
        EntreeStockRequest body = new EntreeStockRequest(magasinId, productFournisseurId, 100, BigDecimal.ZERO, null, null, null);

        mockMvc.perform(post(EntreeStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
