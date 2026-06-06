package org.store.inventaire.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.inventaire.application.dto.BilanInventaireRequest;
import org.store.inventaire.application.dto.InventaireFilter;
import org.store.inventaire.application.dto.InventaireResponse;
import org.store.inventaire.application.dto.LigneInventaireRequest;
import org.store.inventaire.application.dto.LigneInventaireResponse;
import org.store.inventaire.application.dto.LigneInventaireUpdateRequest;
import org.store.inventaire.application.dto.RapportInventaireResponse;
import org.store.inventaire.application.service.IInventaireService;
import org.store.inventaire.domain.enums.InventaireStatut;
import org.store.inventaire.domain.enums.StatutRapport;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.produit.application.dto.ProductSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InventaireControllerTest {

    private MockMvc mockMvc;
    private IInventaireService inventaireService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID inventaireId;
    private UUID magasinId;
    private UUID productFournisseurId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        inventaireService = mock(IInventaireService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new InventaireController(inventaireService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        inventaireId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();
        productId = UUID.randomUUID();
    }

    private InventaireResponse sample(InventaireStatut statut) {
        return new InventaireResponse(
                inventaireId,
                new MagasinSummaryResponse(magasinId, "Magasin Central"),
                statut, LocalDate.now(), null, null, "2026-05-16 10:00:00"
        );
    }

    private LigneInventaireResponse sampleLigne() {
        return new LigneInventaireResponse(
                UUID.randomUUID(), inventaireId, productFournisseurId,
                new ProductSummaryResponse(productId, "Clou 10mm", "CL-10", "Visserie"),
                new org.store.achat.application.dto.FournisseurSummaryResponse(UUID.randomUUID(), "Fournisseur Test"),
                null,
                10, 8, -2,
                new java.math.BigDecimal("10.00")
        );
    }

    @Test
    void should_return_201_when_inventaire_created() throws Exception {
        when(inventaireService.create(magasinId)).thenReturn(sample(InventaireStatut.EN_COURS));

        mockMvc.perform(post(InventaireController.BASE_PATH).param("magasinId", magasinId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(inventaireId.toString()))
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
    }

    @Test
    void should_return_201_when_ligne_added() throws Exception {
        LigneInventaireRequest body = new LigneInventaireRequest(productFournisseurId, 8, new java.math.BigDecimal("10.00"));
        when(inventaireService.addLigne(eq(inventaireId), any(LigneInventaireRequest.class))).thenReturn(sampleLigne());

        mockMvc.perform(post(InventaireController.BASE_PATH + "/" + inventaireId + "/lignes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantiteReelle").value(8))
                .andExpect(jsonPath("$.ecart").value(-2));
    }

    @Test
    void should_return_400_when_ligne_missing_fields() throws Exception {
        LigneInventaireRequest body = new LigneInventaireRequest(null, null, null);

        mockMvc.perform(post(InventaireController.BASE_PATH + "/" + inventaireId + "/lignes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_quantiteReelle_negative() throws Exception {
        LigneInventaireRequest body = new LigneInventaireRequest(productFournisseurId, -1, new java.math.BigDecimal("10.00"));

        mockMvc.perform(post(InventaireController.BASE_PATH + "/" + inventaireId + "/lignes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_when_passer_en_bilan() throws Exception {
        BilanInventaireRequest body = new BilanInventaireRequest(
                new BigDecimal("500.00"), new BigDecimal("1000.00"), LocalDate.now().minusDays(7));
        when(inventaireService.passerEnBilan(eq(inventaireId), any(BilanInventaireRequest.class)))
                .thenReturn(sample(InventaireStatut.BILAN));

        mockMvc.perform(post(InventaireController.BASE_PATH + "/" + inventaireId + "/bilan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("BILAN"));
    }

    @Test
    void should_return_400_when_bilan_missing_fields() throws Exception {
        BilanInventaireRequest body = new BilanInventaireRequest(null, null, null);

        mockMvc.perform(post(InventaireController.BASE_PATH + "/" + inventaireId + "/bilan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_bilan_dateDebutPeriode_future() throws Exception {
        BilanInventaireRequest body = new BilanInventaireRequest(
                new BigDecimal("100.00"), new BigDecimal("100.00"), LocalDate.now().plusDays(1));

        mockMvc.perform(post(InventaireController.BASE_PATH + "/" + inventaireId + "/bilan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_when_cloturer() throws Exception {
        when(inventaireService.cloturer(inventaireId, null)).thenReturn(sample(InventaireStatut.CLOTURE));

        mockMvc.perform(post(InventaireController.BASE_PATH + "/" + inventaireId + "/cloturer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CLOTURE"));
    }

    @Test
    void should_return_200_when_get_rapport() throws Exception {
        RapportInventaireResponse rapport = new RapportInventaireResponse(
                UUID.randomUUID(), inventaireId,
                new BigDecimal("100.00"), new BigDecimal("120.00"), new BigDecimal("20.00"),
                new BigDecimal("500.00"), new BigDecimal("50.00"), new BigDecimal("400.00"),
                "2026-05-09", "2026-05-16",
                new BigDecimal("170.00"), StatutRapport.BENEFICE,
                "2026-05-16 12:00:00"
        );
        when(inventaireService.findRapportByInventaireId(inventaireId)).thenReturn(rapport);

        mockMvc.perform(get(InventaireController.BASE_PATH + "/" + inventaireId + "/rapport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.benefice").value(170.00))
                .andExpect(jsonPath("$.status").value("BENEFICE"))
                .andExpect(jsonPath("$.dateDebutPeriode").value("2026-05-09"))
                .andExpect(jsonPath("$.dateFinPeriode").value("2026-05-16"));
    }

    @Test
    void should_return_200_when_annuler() throws Exception {
        when(inventaireService.annuler(inventaireId)).thenReturn(sample(InventaireStatut.ANNULE));

        mockMvc.perform(post(InventaireController.BASE_PATH + "/" + inventaireId + "/annuler"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULE"));
    }

    @Test
    void should_return_200_when_update_ligne() throws Exception {
        UUID ligneId = UUID.randomUUID();
        LigneInventaireUpdateRequest body = new LigneInventaireUpdateRequest(7);
        when(inventaireService.updateLigne(eq(inventaireId), eq(ligneId), any(LigneInventaireUpdateRequest.class)))
                .thenReturn(sampleLigne());

        mockMvc.perform(put(InventaireController.BASE_PATH + "/" + inventaireId + "/lignes/" + ligneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ecart").value(-2));
    }

    @Test
    void should_return_400_when_update_ligne_quantite_null() throws Exception {
        LigneInventaireUpdateRequest body = new LigneInventaireUpdateRequest(null);

        mockMvc.perform(put(InventaireController.BASE_PATH + "/" + inventaireId + "/lignes/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_204_when_delete_ligne() throws Exception {
        UUID ligneId = UUID.randomUUID();

        mockMvc.perform(delete(InventaireController.BASE_PATH + "/" + inventaireId + "/lignes/" + ligneId))
                .andExpect(status().isNoContent());

        verify(inventaireService).deleteLigne(inventaireId, ligneId);
    }

    @Test
    void should_return_200_with_paginated_lignes() throws Exception {
        Page<LigneInventaireResponse> page = new PageImpl<>(List.of(sampleLigne()), PageRequest.of(0, 10), 1);
        when(inventaireService.findLignes(eq(inventaireId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(InventaireController.BASE_PATH + "/" + inventaireId + "/lignes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].quantiteReelle").value(8))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_with_paginated_inventaires() throws Exception {
        Page<InventaireResponse> page = new PageImpl<>(List.of(sample(InventaireStatut.EN_COURS)), PageRequest.of(0, 10), 1);
        when(inventaireService.findAllByCurrentEntreprise(any(InventaireFilter.class))).thenReturn(page);

        mockMvc.perform(get(InventaireController.BASE_PATH).param("magasinId", magasinId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(inventaireId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(inventaireService.findResponseById(inventaireId)).thenReturn(sample(InventaireStatut.EN_COURS));

        mockMvc.perform(get(InventaireController.BASE_PATH + "/" + inventaireId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inventaireId.toString()));
    }
}
