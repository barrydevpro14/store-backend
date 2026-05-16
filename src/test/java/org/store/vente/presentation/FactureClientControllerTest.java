package org.store.vente.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.application.service.IFactureClientService;
import org.store.vente.application.service.IPaiementVenteService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FactureClientControllerTest {

    private MockMvc mockMvc;
    private IFactureClientService factureClientService;
    private IPaiementVenteService paiementVenteService;

    private UUID magasinId;

    @BeforeEach
    void setUp() {
        factureClientService = mock(IFactureClientService.class);
        paiementVenteService = mock(IPaiementVenteService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        mockMvc = MockMvcBuilders.standaloneSetup(new FactureClientController(factureClientService, paiementVenteService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        magasinId = UUID.randomUUID();
    }

    private FactureClientResponse sampleFacture(UUID id) {
        return new FactureClientResponse(
                id, "FAC-VTE-001", StatutFacture.NON_PAYEE,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                LocalDate.of(2026, 5, 16), LocalDate.of(2026, 5, 30), UUID.randomUUID()
        );
    }

    @Test
    void list_should_return_200_with_paginated_factures() throws Exception {
        Page<FactureClientResponse> page = new PageImpl<>(List.of(sampleFacture(UUID.randomUUID())), PageRequest.of(0, 10), 1);
        when(factureClientService.findAllByCurrentEntreprise(any())).thenReturn(page);

        mockMvc.perform(get(FactureClientController.BASE_PATH).param("magasinId", magasinId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].numero").value("FAC-VTE-001"));
    }

    @Test
    void getById_should_return_200_when_found() throws Exception {
        UUID factureId = UUID.randomUUID();
        when(factureClientService.findResponseById(eq(factureId))).thenReturn(sampleFacture(factureId));

        mockMvc.perform(get(FactureClientController.BASE_PATH + "/" + factureId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("FAC-VTE-001"));
    }

    @Test
    void getById_should_return_406_when_notFound() throws Exception {
        UUID factureId = UUID.randomUUID();
        when(factureClientService.findResponseById(eq(factureId)))
                .thenThrow(new EntityException("factureClient.notFound", factureId));

        mockMvc.perform(get(FactureClientController.BASE_PATH + "/" + factureId))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void listPaiements_should_return_200_with_paginated_paiements() throws Exception {
        UUID factureId = UUID.randomUUID();
        PaiementVenteResponse paiement = new PaiementVenteResponse(
                UUID.randomUUID(), new BigDecimal("500.00"), LocalDate.of(2026, 5, 16),
                MoyenPaiement.CASH, factureId
        );
        Page<PaiementVenteResponse> page = new PageImpl<>(List.of(paiement), PageRequest.of(0, 10), 1);
        when(paiementVenteService.findByFactureId(eq(factureId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(FactureClientController.BASE_PATH + "/" + factureId + "/paiements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].moyen").value("CASH"));
    }
}
