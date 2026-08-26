package org.store.abonnement.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.abonnement.application.dto.PaiementAbonnementDetailsResponse;
import org.store.abonnement.application.dto.PaiementAbonnementFilter;
import org.store.abonnement.application.dto.PaiementAbonnementResponse;
import org.store.abonnement.application.dto.PlanAbonnementSummaryResponse;
import org.store.abonnement.application.dto.PreuvePaiementRequest;
import org.store.abonnement.application.dto.PreuvePaiementResponse;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.abonnement.application.service.IPreuvePaiementService;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.enums.StatutPreuvePaiement;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaiementAbonnementControllerTest {

    private MockMvc mockMvc;
    private IPaiementAbonnementService paiementAbonnementService;
    private IPreuvePaiementService preuvePaiementService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID paiementId;
    private UUID abonnementId;
    private UUID moyenPaiementId;

    @BeforeEach
    void setUp() {
        paiementAbonnementService = mock(IPaiementAbonnementService.class);
        preuvePaiementService = mock(IPreuvePaiementService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new PaiementAbonnementController(paiementAbonnementService, preuvePaiementService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        paiementId = UUID.randomUUID();
        abonnementId = UUID.randomUUID();
        moyenPaiementId = UUID.randomUUID();
    }

    private PaiementAbonnementResponse sample(StatutPaiementAbonnement statut) {
        PlanAbonnementSummaryResponse plan = new PlanAbonnementSummaryResponse(
                UUID.randomUUID(), "Premium");
        return new PaiementAbonnementResponse(
                paiementId, abonnementId, "ACME", plan,
                new BigDecimal("238800"), new BigDecimal("0"), new BigDecimal("238800"),
                LocalDate.now(),
                LocalDate.now(),
                statut, LocalDateTime.now());
    }

    private PreuvePaiementResponse samplePreuve(StatutPreuvePaiement statut) {
        return new PreuvePaiementResponse(
                UUID.randomUUID(), paiementId, LocalDate.now(),
                new MoyenPaiementResponse(moyenPaiementId, "Wave", true),
                "TXN-001", UUID.randomUUID(), statut, null, LocalDateTime.now());
    }

    @Test
    void should_return_201_when_payer_ok() throws Exception {
        when(preuvePaiementService.create(eq(paiementId), any(PreuvePaiementRequest.class), any()))
                .thenReturn(samplePreuve(StatutPreuvePaiement.EN_ATTENTE_VALIDATION));

        MockMultipartFile dataFile = new MockMultipartFile(
                "data", "data.json", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new PreuvePaiementRequest(moyenPaiementId, "TXN-001")));
        MockMultipartFile preuve = new MockMultipartFile(
                "file", "preuve.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart(PaiementAbonnementController.BASE_PATH + "/" + paiementId + "/payer")
                        .file(dataFile)
                        .file(preuve))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_VALIDATION"))
                .andExpect(jsonPath("$.paiementAbonnementId").value(paiementId.toString()))
                .andExpect(jsonPath("$.referenceTransaction").value("TXN-001"));
    }

    @Test
    void should_return_200_with_page_when_list() throws Exception {
        Page<PaiementAbonnementResponse> page = new PageImpl<>(
                List.of(sample(StatutPaiementAbonnement.FACTURE_GENEREE)),
                PageRequest.of(0, 10), 1);
        when(paiementAbonnementService.findAll(any(PaiementAbonnementFilter.class))).thenReturn(page);

        mockMvc.perform(get(PaiementAbonnementController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].statut").value("FACTURE_GENEREE"))
                .andExpect(jsonPath("$.content[0].entrepriseSigle").value("ACME"))
                .andExpect(jsonPath("$.content[0].plan.nom").value("Premium"));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        PaiementAbonnementDetailsResponse details = new PaiementAbonnementDetailsResponse(
                sample(StatutPaiementAbonnement.FACTURE_GENEREE),
                List.of(samplePreuve(StatutPreuvePaiement.EN_ATTENTE_VALIDATION)));
        when(paiementAbonnementService.findDetailsById(eq(paiementId)))
                .thenReturn(details);

        mockMvc.perform(get(PaiementAbonnementController.BASE_PATH + "/" + paiementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facture.id").value(paiementId.toString()))
                .andExpect(jsonPath("$.preuves").isArray())
                .andExpect(jsonPath("$.preuves[0].referenceTransaction").value("TXN-001"));
    }
}
