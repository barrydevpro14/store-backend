package org.store.vente.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.vente.application.dto.CaisseResumeResponse;
import org.store.vente.application.dto.TopProduitResponse;
import org.store.vente.application.service.ICaisseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CaisseControllerTest {

    private MockMvc mockMvc;
    private ICaisseService caisseService;

    private UUID magasinId;

    @BeforeEach
    void setUp() {
        caisseService = mock(ICaisseService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        mockMvc = MockMvcBuilders.standaloneSetup(new CaisseController(caisseService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .build();

        magasinId = UUID.randomUUID();
    }

    @Test
    void resume_should_return_200_with_aggregated_values() throws Exception {
        CaisseResumeResponse response = new CaisseResumeResponse(
                magasinId, LocalDate.of(2026, 5, 16),
                27L, 312L,
                new BigDecimal("145000.00"), new BigDecimal("98500.00")
        );
        when(caisseService.getResume(any())).thenReturn(response);

        mockMvc.perform(get(CaisseController.BASE_PATH + "/resume")
                        .param("magasinId", magasinId.toString())
                        .param("date", "2026-05-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCommandes").value(27))
                .andExpect(jsonPath("$.nombreProduits").value(312))
                .andExpect(jsonPath("$.totalCommandes").value(145000.00))
                .andExpect(jsonPath("$.totalPaiements").value(98500.00));
    }

    @Test
    void topProduits_should_return_200_with_default_nombre_3() throws Exception {
        List<TopProduitResponse> top = List.of(
                new TopProduitResponse(UUID.randomUUID(), "Clou 10mm", "CL-10", 250L, new BigDecimal("12500.00")),
                new TopProduitResponse(UUID.randomUUID(), "Vis M6", "VS-M6", 180L, new BigDecimal("9000.00")),
                new TopProduitResponse(UUID.randomUUID(), "Boulon 8mm", "BL-08", 95L, new BigDecimal("4750.00"))
        );
        when(caisseService.findTopProduits(any())).thenReturn(top);

        mockMvc.perform(get(CaisseController.BASE_PATH + "/top-produits")
                        .param("magasinId", magasinId.toString())
                        .param("date", "2026-05-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].nom").value("Clou 10mm"))
                .andExpect(jsonPath("$[0].quantiteVendue").value(250));
    }

    @Test
    void topProduits_should_accept_custom_nombre_and_omit_date() throws Exception {
        when(caisseService.findTopProduits(any())).thenReturn(List.of());

        mockMvc.perform(get(CaisseController.BASE_PATH + "/top-produits")
                        .param("magasinId", magasinId.toString())
                        .param("nombre", "10"))
                .andExpect(status().isOk());
    }
}
