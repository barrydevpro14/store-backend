package org.store.abonnement.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.application.dto.CurrentAbonnementResponse;
import org.store.abonnement.application.dto.PlanAbonnementSummaryResponse;
import org.store.abonnement.application.dto.PlanFeaturesResponse;
import org.store.abonnement.application.dto.RenouvellementAutoRequest;
import org.store.abonnement.application.dto.SubscribeRequest;
import org.store.abonnement.application.dto.SubscribeResponse;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.dto.SubscriptionTypeSummaryResponse;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AbonnementControllerTest {

    private MockMvc mockMvc;
    private IAbonnementService abonnementService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID planId;
    private UUID typeId;
    private UUID abonnementId;
    private UUID entrepriseId;

    @BeforeEach
    void setUp() {
        abonnementService = mock(IAbonnementService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AbonnementController(abonnementService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        planId = UUID.randomUUID();
        typeId = UUID.randomUUID();
        abonnementId = UUID.randomUUID();
        entrepriseId = UUID.randomUUID();
    }

    private SubscribeResponse sampleResponse() {
        AbonnementResponse abonnement = new AbonnementResponse(
                abonnementId, entrepriseId,
                new PlanAbonnementSummaryResponse(planId, "Pro", new BigDecimal("19900")),
                new SubscriptionTypeSummaryResponse(typeId, "Annuel", 12),
                null, null, false, false, AbonnementStatut.EN_ATTENTE);
        SubscriptionAmountBreakdown breakdown = new SubscriptionAmountBreakdown(
                new BigDecimal("238800.00"), new BigDecimal("0"),
                new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("238800.00"));
        return new SubscribeResponse(abonnement, breakdown, null, null);
    }

    @Test
    void should_return_201_when_subscribe_ok() throws Exception {
        SubscribeRequest body = new SubscribeRequest(planId, typeId, null, true);
        when(abonnementService.subscribe(any(SubscribeRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post(AbonnementController.BASE_PATH + "/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.abonnement.id").value(abonnementId.toString()))
                .andExpect(jsonPath("$.abonnement.statut").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.breakdown.montantAPayer").value(238800.00));
    }

    @Test
    void should_return_400_when_planId_null() throws Exception {
        String json = """
                { "planId":null, "typeId":"%s", "renouvellementAuto":true }
                """.formatted(typeId);

        mockMvc.perform(post(AbonnementController.BASE_PATH + "/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_typeId_null() throws Exception {
        String json = """
                { "planId":"%s", "typeId":null, "renouvellementAuto":true }
                """.formatted(planId);

        mockMvc.perform(post(AbonnementController.BASE_PATH + "/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_when_admin_lists_all() throws Exception {
        AbonnementResponse a = new AbonnementResponse(
                abonnementId, entrepriseId,
                new PlanAbonnementSummaryResponse(planId, "Pro", new BigDecimal("19900")),
                new SubscriptionTypeSummaryResponse(typeId, "Annuel", 12),
                null, null, false, false, AbonnementStatut.EN_ATTENTE);
        Page<AbonnementResponse> page = new PageImpl<>(List.of(a), PageRequest.of(0, 10), 1);
        when(abonnementService.findAll(any(AbonnementFilter.class))).thenReturn(page);

        mockMvc.perform(get(AbonnementController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(abonnementId.toString()));
    }

    @Test
    void should_return_200_when_proprio_lists_history() throws Exception {
        Page<AbonnementResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(abonnementService.findMyHistory(any(AbonnementFilter.class))).thenReturn(page);

        mockMvc.perform(get(AbonnementController.BASE_PATH + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void should_return_200_with_current_subscription() throws Exception {
        AbonnementResponse abonnement = new AbonnementResponse(
                abonnementId, entrepriseId,
                new PlanAbonnementSummaryResponse(planId, "Pro", new BigDecimal("19900")),
                new SubscriptionTypeSummaryResponse(typeId, "Annuel", 12),
                null, null, true, false, AbonnementStatut.ACTIF);
        CurrentAbonnementResponse current = new CurrentAbonnementResponse(
                abonnement, 25L, false,
                new PlanFeaturesResponse(true, true, true, false, 1, 3));

        when(abonnementService.findMyCurrent()).thenReturn(current);

        mockMvc.perform(get(AbonnementController.BASE_PATH + "/me/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.joursRestants").value(25))
                .andExpect(jsonPath("$.isTrial").value(false))
                .andExpect(jsonPath("$.fonctionnalites.nombreMagasinsMax").value(1));
    }

    @Test
    void should_return_200_when_toggling_renouvellement_auto() throws Exception {
        AbonnementResponse abonnement = new AbonnementResponse(
                abonnementId, entrepriseId,
                new PlanAbonnementSummaryResponse(planId, "Pro", new BigDecimal("19900")),
                new SubscriptionTypeSummaryResponse(typeId, "Annuel", 12),
                null, null, true, true, AbonnementStatut.ACTIF);
        when(abonnementService.updateRenouvellementAuto(eq(abonnementId), any(RenouvellementAutoRequest.class)))
                .thenReturn(abonnement);

        mockMvc.perform(patch(AbonnementController.BASE_PATH + "/" + abonnementId + "/renouvellement-auto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "renouvellementAuto": true }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.renouvellementAuto").value(true));
    }
}
