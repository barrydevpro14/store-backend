package org.store.vente.presentation;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import org.store.vente.application.dto.CommandeVenteFilter;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.application.service.ICommandeVenteService;
import org.store.vente.domain.enums.CommandeVenteStatut;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CommandeVenteControllerTest {

    private MockMvc mockMvc;
    private ICommandeVenteService commandeVenteService;

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
                UUID.randomUUID(), "VTE-AUTO-001", CommandeVenteStatut.VALIDATE,
                null, new MagasinSummaryResponse(magasinId, "Magasin Central"),
                null, LocalDate.of(2026, 5, 16),
                new BigDecimal("150.00"), BigDecimal.ZERO,
                null, "2026-05-16 10:00:00"
        );
    }

    private CommandeVenteResponse sampleDetailWithUser(UUID id) {
        return new CommandeVenteResponse(
                id, "VTE-AUTO-002", CommandeVenteStatut.VALIDATE,
                null, new MagasinSummaryResponse(magasinId, "Magasin Central"),
                new UserSummaryResponse(UUID.randomUUID(), "Diop Awa"),
                LocalDate.of(2026, 5, 16),
                new BigDecimal("150.00"), BigDecimal.ZERO,
                null, "2026-05-16 10:00:00"
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
    void list_should_forward_all_filter_params_to_service() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID vendeurId = UUID.randomUUID();
        Page<CommandeVenteResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 25), 0);
        when(commandeVenteService.findAllByCurrentEntreprise(any())).thenReturn(page);

        mockMvc.perform(get(CommandeVenteController.BASE_PATH)
                        .param("magasinId", magasinId.toString())
                        .param("clientId", clientId.toString())
                        .param("vendeurId", vendeurId.toString())
                        .param("statut", "VALIDATE")
                        .param("reference", "VTE-2026")
                        .param("montantMin", "100.00")
                        .param("montantMax", "5000.00")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .param("page", "1")
                        .param("size", "25"))
                .andExpect(status().isOk());

        ArgumentCaptor<CommandeVenteFilter> captor = ArgumentCaptor.forClass(CommandeVenteFilter.class);
        verify(commandeVenteService).findAllByCurrentEntreprise(captor.capture());
        CommandeVenteFilter captured = captor.getValue();
        assertThat(captured.magasinId()).isEqualTo(magasinId);
        assertThat(captured.clientId()).isEqualTo(clientId);
        assertThat(captured.vendeurId()).isEqualTo(vendeurId);
        assertThat(captured.statut()).isEqualTo("VALIDATE");
        assertThat(captured.reference()).isEqualTo("VTE-2026");
        assertThat(captured.montantMin()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(captured.montantMax()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(captured.startDate()).isEqualTo("2026-05-01");
        assertThat(captured.endDate()).isEqualTo("2026-05-31");
        assertThat(captured.page()).isEqualTo(1);
        assertThat(captured.size()).isEqualTo(25);
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
