package org.store.achat.presentation;

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
import org.store.achat.application.dto.FactureAchatEcheanceFilter;
import org.store.achat.application.dto.FactureAchatFilter;
import org.store.achat.application.dto.FactureAchatResponse;
import org.store.achat.application.dto.PaiementAchatRequest;
import org.store.achat.application.dto.PaiementAchatResponse;
import org.store.achat.application.service.IFactureAchatService;
import org.store.achat.application.service.IPaiementAchatService;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FactureAchatControllerTest {

    private MockMvc mockMvc;
    private IFactureAchatService factureAchatService;
    private IPaiementAchatService paiementAchatService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID factureId;
    private UUID magasinId;

    @BeforeEach
    void setUp() {
        factureAchatService = mock(IFactureAchatService.class);
        paiementAchatService = mock(IPaiementAchatService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new FactureAchatController(factureAchatService, paiementAchatService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        factureId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
    }

    private FactureAchatResponse sampleFacture() {
        return new FactureAchatResponse(factureId, "FAC-001", StatutFacture.NON_PAYEE,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                LocalDate.of(2026, 5, 15), LocalDate.of(2026, 6, 15), UUID.randomUUID());
    }

    @Test
    void should_return_200_with_facture_listing() throws Exception {
        Page<FactureAchatResponse> page = new PageImpl<>(List.of(sampleFacture()), PageRequest.of(0, 10), 1);
        when(factureAchatService.findAllByCurrentEntreprise(any(FactureAchatFilter.class))).thenReturn(page);

        mockMvc.perform(get(FactureAchatController.BASE_PATH).param("magasinId", magasinId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].numero").value("FAC-001"));
    }

    @Test
    void should_return_200_with_echeances() throws Exception {
        Page<FactureAchatResponse> page = new PageImpl<>(List.of(sampleFacture()), PageRequest.of(0, 10), 1);
        when(factureAchatService.findEcheances(any(FactureAchatEcheanceFilter.class))).thenReturn(page);

        mockMvc.perform(get(FactureAchatController.BASE_PATH + "/echeances")
                        .param("magasinId", magasinId.toString())
                        .param("startDate", "2026-06-01")
                        .param("endDate", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].statut").value("NON_PAYEE"));
    }

    @Test
    void should_return_201_when_paiement_created() throws Exception {
        PaiementAchatResponse paiement = new PaiementAchatResponse(UUID.randomUUID(), factureId,
                new BigDecimal("400.00"), LocalDate.of(2026, 5, 15), MoyenPaiement.CASH, "2026-05-15 10:00:00");
        when(paiementAchatService.create(eq(factureId), any(PaiementAchatRequest.class))).thenReturn(paiement);

        PaiementAchatRequest body = new PaiementAchatRequest(new BigDecimal("400.00"), LocalDate.of(2026, 5, 15), MoyenPaiement.CASH);

        mockMvc.perform(post(FactureAchatController.BASE_PATH + "/" + factureId + "/paiements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.montant").value(400.00))
                .andExpect(jsonPath("$.moyen").value("CASH"));
    }

    @Test
    void should_return_200_with_paiements_listing() throws Exception {
        Page<PaiementAchatResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(paiementAchatService.findByFactureId(eq(factureId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(FactureAchatController.BASE_PATH + "/" + factureId + "/paiements"))
                .andExpect(status().isOk());

        verify(paiementAchatService).findByFactureId(eq(factureId), any(Pageable.class));
    }
}
