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
import org.store.abonnement.application.dto.PaiementAbonnementFilter;
import org.store.abonnement.application.dto.PaiementAbonnementRequest;
import org.store.abonnement.application.dto.PaiementAbonnementResponse;
import org.store.abonnement.application.dto.PlanAbonnementSummaryResponse;
import org.store.abonnement.application.dto.RejectPaiementRequest;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaiementAbonnementControllerTest {

    private MockMvc mockMvc;
    private IPaiementAbonnementService paiementAbonnementService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID paiementId;
    private UUID abonnementId;

    @BeforeEach
    void setUp() {
        paiementAbonnementService = mock(IPaiementAbonnementService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new PaiementAbonnementController(paiementAbonnementService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        paiementId = UUID.randomUUID();
        abonnementId = UUID.randomUUID();
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

    @Test
    void should_return_201_when_payer_ok() throws Exception {
        when(paiementAbonnementService.payer(eq(paiementId), any(PaiementAbonnementRequest.class), any()))
                .thenReturn(sample(StatutPaiementAbonnement.EN_ATTENTE_VALIDATION));

        MockMultipartFile dataFile = new MockMultipartFile(
                "data", "data.json", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(new PaiementAbonnementRequest(
                        java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"),
                        "TXN-001",
                        LocalDate.now())));
        MockMultipartFile preuve = new MockMultipartFile(
                "file", "preuve.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3});

        mockMvc.perform(multipart(PaiementAbonnementController.BASE_PATH + "/" + paiementId + "/payer")
                        .file(dataFile)
                        .file(preuve))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE_VALIDATION"));
    }

    @Test
    void should_return_200_with_page_when_list() throws Exception {
        Page<PaiementAbonnementResponse> page = new PageImpl<>(
                List.of(sample(StatutPaiementAbonnement.EN_ATTENTE_VALIDATION)),
                PageRequest.of(0, 10), 1);
        when(paiementAbonnementService.findAll(any(PaiementAbonnementFilter.class))).thenReturn(page);

        mockMvc.perform(get(PaiementAbonnementController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].statut").value("EN_ATTENTE_VALIDATION"))
                .andExpect(jsonPath("$.content[0].entrepriseSigle").value("ACME"))
                .andExpect(jsonPath("$.content[0].plan.nom").value("Premium"));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(paiementAbonnementService.findResponseById(eq(paiementId)))
                .thenReturn(sample(StatutPaiementAbonnement.EN_ATTENTE_VALIDATION));

        mockMvc.perform(get(PaiementAbonnementController.BASE_PATH + "/" + paiementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paiementId.toString()));
    }

    @Test
    void should_return_200_when_admin_validates() throws Exception {
        when(paiementAbonnementService.validate(eq(paiementId)))
                .thenReturn(sample(StatutPaiementAbonnement.VALIDE));

        mockMvc.perform(patch(PaiementAbonnementController.BASE_PATH + "/" + paiementId + "/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDE"));
    }

    @Test
    void should_return_200_when_admin_rejects() throws Exception {
        when(paiementAbonnementService.reject(eq(paiementId), any(RejectPaiementRequest.class)))
                .thenReturn(sample(StatutPaiementAbonnement.REJETE));

        mockMvc.perform(patch(PaiementAbonnementController.BASE_PATH + "/" + paiementId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RejectPaiementRequest("Preuve illisible"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("REJETE"));
    }

    @Test
    void should_return_400_when_reject_without_motif() throws Exception {
        mockMvc.perform(patch(PaiementAbonnementController.BASE_PATH + "/" + paiementId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "motifRejet": "" }
                                """))
                .andExpect(status().isBadRequest());
    }
}
