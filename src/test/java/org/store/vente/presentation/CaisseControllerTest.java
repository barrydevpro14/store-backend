package org.store.vente.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.vente.application.dto.CaisseResumeResponse;
import org.store.vente.application.service.ICaisseService;

import java.math.BigDecimal;
import java.time.LocalDate;
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
}
