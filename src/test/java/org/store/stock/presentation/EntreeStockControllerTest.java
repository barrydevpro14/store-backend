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
import org.store.stock.application.dto.LigneEntreeStockRequest;
import org.store.stock.application.service.IEntreeStockService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
    private UUID qualityId;
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
        qualityId = UUID.randomUUID();
        fournisseurId = UUID.randomUUID();
    }

    private EntreeStockResponse sampleResponse() {
        return new EntreeStockResponse(
                entreeStockId,
                new MagasinSummaryResponse(magasinId, "Magasin Central"),
                new ProductSummaryResponse(productId, "Clou 10mm", "CL-10", null),
                new FournisseurSummaryResponse(fournisseurId, "Fournisseur anonyme"),
                100, 100,
                new BigDecimal("10.00"), "LOT-001",
                "2027-05-14",
                "2026-05-14 10:00:00"
        );
    }

    private LigneEntreeStockRequest validLigne() {
        return new LigneEntreeStockRequest(productId, qualityId, 100, new BigDecimal("10.00"), new BigDecimal("15.00"), "LOT-001", LocalDate.now().plusYears(1));
    }

    private EntreeStockRequest validBody() {
        return new EntreeStockRequest(magasinId, fournisseurId, List.of(validLigne()));
    }

    @Test
    void should_return_201_with_list_when_created() throws Exception {
        when(entreeStockService.create(any(EntreeStockRequest.class))).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(post(EntreeStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(entreeStockId.toString()))
                .andExpect(jsonPath("$[0].magasin.id").value(magasinId.toString()))
                .andExpect(jsonPath("$[0].produit.id").value(productId.toString()))
                .andExpect(jsonPath("$[0].fournisseur.id").value(fournisseurId.toString()))
                .andExpect(jsonPath("$[0].quantiteInitiale").value(100))
                .andExpect(jsonPath("$[0].prixAchat").value(10.00))
                .andExpect(jsonPath("$[0].numeroLot").value("LOT-001"));
    }

    @Test
    void should_return_400_when_magasinId_null() throws Exception {
        EntreeStockRequest body = new EntreeStockRequest(null, fournisseurId, List.of(validLigne()));

        mockMvc.perform(post(EntreeStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_fournisseurId_null() throws Exception {
        EntreeStockRequest body = new EntreeStockRequest(magasinId, null, List.of(validLigne()));

        mockMvc.perform(post(EntreeStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_lignes_empty() throws Exception {
        EntreeStockRequest body = new EntreeStockRequest(magasinId, fournisseurId, List.of());

        mockMvc.perform(post(EntreeStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_ligne_productId_null() throws Exception {
        LigneEntreeStockRequest ligne = new LigneEntreeStockRequest(null, qualityId, 100, new BigDecimal("10.00"), new BigDecimal("15.00"), null, null);
        EntreeStockRequest body = new EntreeStockRequest(magasinId, fournisseurId, List.of(ligne));

        mockMvc.perform(post(EntreeStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_ligne_qualityId_null() throws Exception {
        LigneEntreeStockRequest ligne = new LigneEntreeStockRequest(productId, null, 100, new BigDecimal("10.00"), new BigDecimal("15.00"), null, null);
        EntreeStockRequest body = new EntreeStockRequest(magasinId, fournisseurId, List.of(ligne));

        mockMvc.perform(post(EntreeStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_ligne_quantite_zero_or_negative() throws Exception {
        LigneEntreeStockRequest ligne = new LigneEntreeStockRequest(productId, qualityId, 0, new BigDecimal("10.00"), new BigDecimal("15.00"), null, null);
        EntreeStockRequest body = new EntreeStockRequest(magasinId, fournisseurId, List.of(ligne));

        mockMvc.perform(post(EntreeStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_ligne_prixAchat_zero() throws Exception {
        LigneEntreeStockRequest ligne = new LigneEntreeStockRequest(productId, qualityId, 100, BigDecimal.ZERO, new BigDecimal("15.00"), null, null);
        EntreeStockRequest body = new EntreeStockRequest(magasinId, fournisseurId, List.of(ligne));

        mockMvc.perform(post(EntreeStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_ligne_prixVente_zero() throws Exception {
        LigneEntreeStockRequest ligne = new LigneEntreeStockRequest(productId, qualityId, 100, new BigDecimal("10.00"), BigDecimal.ZERO, null, null);
        EntreeStockRequest body = new EntreeStockRequest(magasinId, fournisseurId, List.of(ligne));

        mockMvc.perform(post(EntreeStockController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
