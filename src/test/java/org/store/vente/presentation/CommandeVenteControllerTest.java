package org.store.vente.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.store.common.dto.UserSummaryResponse;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.application.service.ICommandeVenteService;
import org.store.vente.domain.enums.CommandeVenteStatut;

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

class CommandeVenteControllerTest {

    private MockMvc mockMvc;
    private ICommandeVenteService commandeVenteService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID magasinId;

    @BeforeEach
    void setUp() {
        commandeVenteService = mock(ICommandeVenteService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        mockMvc = MockMvcBuilders.standaloneSetup(new CommandeVenteController(commandeVenteService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .build();

        magasinId = UUID.randomUUID();
    }

    private CommandeVenteResponse sampleListItem() {
        return new CommandeVenteResponse(
                UUID.randomUUID(), "VTE-AUTO-001", CommandeVenteStatut.DELIVERED,
                null, new MagasinSummaryResponse(magasinId, "Magasin Central"),
                null, LocalDate.of(2026, 5, 16),
                new BigDecimal("150.00"), BigDecimal.ZERO,
                "2026-05-16 10:00:00"
        );
    }

    private CommandeVenteResponse sampleDetailWithUser(UUID id) {
        return new CommandeVenteResponse(
                id, "VTE-AUTO-002", CommandeVenteStatut.DELIVERED,
                null, new MagasinSummaryResponse(magasinId, "Magasin Central"),
                new UserSummaryResponse(UUID.randomUUID(), "Diop Awa"),
                LocalDate.of(2026, 5, 16),
                new BigDecimal("150.00"), BigDecimal.ZERO,
                "2026-05-16 10:00:00"
        );
    }

    @Test
    void list_should_return_200_with_paginated_results_no_user_field() throws Exception {
        Page<CommandeVenteResponse> page = new PageImpl<>(List.of(sampleListItem()), PageRequest.of(0, 10), 1);
        when(commandeVenteService.findAllByCurrentEntreprise(any())).thenReturn(page);

        mockMvc.perform(get(CommandeVenteController.BASE_PATH).param("magasinId", magasinId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].reference").value("VTE-AUTO-001"))
                .andExpect(jsonPath("$.content[0].user").doesNotExist());
    }

    @Test
    void getById_should_return_200_with_user_resolved() throws Exception {
        UUID commandeId = UUID.randomUUID();
        when(commandeVenteService.findResponseById(eq(commandeId))).thenReturn(sampleDetailWithUser(commandeId));

        mockMvc.perform(get(CommandeVenteController.BASE_PATH + "/" + commandeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("VTE-AUTO-002"))
                .andExpect(jsonPath("$.user.nomComplet").value("Diop Awa"));
    }

    @Test
    void getById_should_return_406_when_commande_notFound() throws Exception {
        UUID commandeId = UUID.randomUUID();
        when(commandeVenteService.findResponseById(eq(commandeId)))
                .thenThrow(new EntityException("commandeVente.notFound", commandeId));

        mockMvc.perform(get(CommandeVenteController.BASE_PATH + "/" + commandeId))
                .andExpect(status().isNotAcceptable());
    }
}
