package org.store.vente.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.vente.application.dto.FactureClientFilter;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.application.dto.PaiementVenteRequest;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.application.service.IFactureClientService;
import org.store.vente.application.service.IInvoicePdfService;
import org.store.vente.application.service.IPaiementVenteService;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FactureClientControllerTest {

    private MockMvc mockMvc;
    private IFactureClientService factureClientService;
    private IPaiementVenteService paiementVenteService;
    private IInvoicePdfService invoicePdfService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID magasinId;

    @BeforeEach
    void setUp() {
        factureClientService = mock(IFactureClientService.class);
        paiementVenteService = mock(IPaiementVenteService.class);
        invoicePdfService = mock(IInvoicePdfService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new FactureClientController(factureClientService, paiementVenteService, invoicePdfService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setValidator(validator)
                .build();

        magasinId = UUID.randomUUID();
    }

    private FactureClientResponse sampleFacture(UUID id) {
        return new FactureClientResponse(
                id, "FAC-VTE-001", StatutFacture.NON_PAYEE,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                LocalDate.of(2026, 5, 16), LocalDate.of(2026, 5, 30), UUID.randomUUID(), null, null
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
    void list_should_forward_all_filter_params_to_service() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID vendeurId = UUID.randomUUID();
        Page<FactureClientResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 25), 0);
        when(factureClientService.findAllByCurrentEntreprise(any())).thenReturn(page);

        mockMvc.perform(get(FactureClientController.BASE_PATH)
                        .param("magasinId", magasinId.toString())
                        .param("clientId", clientId.toString())
                        .param("vendeurId", vendeurId.toString())
                        .param("statut", "NON_PAYEE")
                        .param("numero", "FAC-VTE-2026")
                        .param("montantMin", "100.00")
                        .param("montantMax", "5000.00")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .param("page", "1")
                        .param("size", "25"))
                .andExpect(status().isOk());

        ArgumentCaptor<FactureClientFilter> captor = ArgumentCaptor.forClass(FactureClientFilter.class);
        verify(factureClientService).findAllByCurrentEntreprise(captor.capture());
        FactureClientFilter captured = captor.getValue();
        assertThat(captured.magasinId()).isEqualTo(magasinId);
        assertThat(captured.clientId()).isEqualTo(clientId);
        assertThat(captured.vendeurId()).isEqualTo(vendeurId);
        assertThat(captured.statut()).isEqualTo("NON_PAYEE");
        assertThat(captured.numero()).isEqualTo("FAC-VTE-2026");
        assertThat(captured.montantMin()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(captured.montantMax()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(captured.startDate()).isEqualTo("2026-05-01");
        assertThat(captured.endDate()).isEqualTo("2026-05-31");
        assertThat(captured.page()).isEqualTo(1);
        assertThat(captured.size()).isEqualTo(25);
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

    @Test
    void createPaiement_should_return_201_when_paiement_added() throws Exception {
        UUID factureId = UUID.randomUUID();
        PaiementVenteRequest body = new PaiementVenteRequest(new BigDecimal("400.00"), MoyenPaiement.CASH.name(), null);
        PaiementVenteResponse response = new PaiementVenteResponse(
                UUID.randomUUID(), new BigDecimal("400.00"), LocalDate.of(2026, 5, 16),
                MoyenPaiement.CASH, factureId
        );
        when(paiementVenteService.create(eq(factureId), any(PaiementVenteRequest.class))).thenReturn(response);

        mockMvc.perform(post(FactureClientController.BASE_PATH + "/" + factureId + "/paiements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montant").value(400.00))
                .andExpect(jsonPath("$.moyen").value("CASH"));
    }

    @Test
    void createPaiement_should_return_400_when_montant_missing() throws Exception {
        UUID factureId = UUID.randomUUID();
        PaiementVenteRequest body = new PaiementVenteRequest(null, MoyenPaiement.CASH.name(), null);

        mockMvc.perform(post(FactureClientController.BASE_PATH + "/" + factureId + "/paiements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
