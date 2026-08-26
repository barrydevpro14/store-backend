package org.store.abonnement.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.abonnement.application.dto.PreuvePaiementResponse;
import org.store.abonnement.application.dto.RejectPaiementRequest;
import org.store.abonnement.application.service.IPreuvePaiementService;
import org.store.abonnement.domain.enums.StatutPreuvePaiement;
import org.store.common.dto.ImageDownloadResponse;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.paiement.application.dto.MoyenPaiementResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PreuvePaiementControllerTest {

    private MockMvc mockMvc;
    private IPreuvePaiementService preuvePaiementService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID preuveId;
    private UUID paiementAbonnementId;

    @BeforeEach
    void setUp() {
        preuvePaiementService = mock(IPreuvePaiementService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new PreuvePaiementController(preuvePaiementService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        preuveId = UUID.randomUUID();
        paiementAbonnementId = UUID.randomUUID();
    }

    private PreuvePaiementResponse sample(StatutPreuvePaiement statut, String motifRejet) {
        return new PreuvePaiementResponse(
                preuveId, paiementAbonnementId, LocalDate.now(),
                new MoyenPaiementResponse(UUID.randomUUID(), "Wave", true),
                "TXN-001", UUID.randomUUID(), statut, motifRejet, LocalDateTime.now());
    }

    @Test
    void validate_should_return_200() throws Exception {
        when(preuvePaiementService.validate(eq(preuveId)))
                .thenReturn(sample(StatutPreuvePaiement.VALIDEE, null));

        mockMvc.perform(patch(PreuvePaiementController.BASE_PATH + "/" + preuveId + "/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VALIDEE"));
    }

    @Test
    void reject_should_return_200() throws Exception {
        when(preuvePaiementService.reject(eq(preuveId), any(RejectPaiementRequest.class)))
                .thenReturn(sample(StatutPreuvePaiement.REJETEE, "Preuve illisible"));

        mockMvc.perform(patch(PreuvePaiementController.BASE_PATH + "/" + preuveId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RejectPaiementRequest("Preuve illisible"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("REJETEE"))
                .andExpect(jsonPath("$.motifRejet").value("Preuve illisible"));
    }

    @Test
    void reject_should_return_400_when_motif_blank() throws Exception {
        mockMvc.perform(patch(PreuvePaiementController.BASE_PATH + "/" + preuveId + "/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "motifRejet": "" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPreuve_should_return_200_with_image_bytes() throws Exception {
        byte[] content = new byte[]{1, 2, 3};
        when(preuvePaiementService.getImage(eq(preuveId)))
                .thenReturn(new ImageDownloadResponse(content, MediaType.IMAGE_PNG_VALUE));

        mockMvc.perform(get(PreuvePaiementController.BASE_PATH + "/" + preuveId + "/preuve"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .contentType(MediaType.IMAGE_PNG))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .bytes(content));
    }
}
