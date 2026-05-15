package org.store.achat.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatResponse;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.application.dto.FactureAchatCreateRequest;
import org.store.achat.application.dto.FactureAchatResponse;
import org.store.achat.application.dto.FournisseurSummaryResponse;
import org.store.achat.application.dto.LigneAchatRequest;
import org.store.achat.application.service.IAchatService;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.magasin.application.dto.MagasinSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AchatControllerTest {

    private MockMvc mockMvc;
    private IAchatService achatService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID magasinId;
    private UUID fournisseurId;
    private UUID productFournisseurId;

    @BeforeEach
    void setUp() {
        achatService = mock(IAchatService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AchatController(achatService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        magasinId = UUID.randomUUID();
        fournisseurId = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();
    }

    private AchatRequest validBody() {
        return new AchatRequest(magasinId, fournisseurId, LocalDate.of(2026, 5, 15),
                List.of(new LigneAchatRequest(productFournisseurId, 100, new BigDecimal("10.00"), new BigDecimal("15.00"), "LOT-001", null)),
                new FactureAchatCreateRequest("FAC-001", LocalDate.of(2026, 5, 15), null));
    }

    private AchatResponse sample() {
        CommandeAchatResponse cmd = new CommandeAchatResponse(
                UUID.randomUUID(), "CMD-AUTO", CommandeAchatStatut.RECEPTIONNEE,
                new FournisseurSummaryResponse(fournisseurId, "Fournisseur Chine"),
                new MagasinSummaryResponse(magasinId, "Magasin Central"),
                LocalDate.of(2026, 5, 15), List.of(), "2026-05-15 10:00:00"
        );
        FactureAchatResponse fac = new FactureAchatResponse(
                UUID.randomUUID(), "FAC-001", StatutFacture.NON_PAYEE,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                LocalDate.of(2026, 5, 15), null, cmd.id()
        );
        return new AchatResponse(cmd, fac);
    }

    @Test
    void should_return_201_when_purchase_created() throws Exception {
        when(achatService.create(any(AchatRequest.class))).thenReturn(sample());

        mockMvc.perform(post(AchatController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commande.reference").value("CMD-AUTO"))
                .andExpect(jsonPath("$.facture.numero").value("FAC-001"))
                .andExpect(jsonPath("$.facture.statut").value("NON_PAYEE"));
    }

    @Test
    void should_return_200_when_get_purchase_details() throws Exception {
        UUID commandeId = UUID.randomUUID();
        CommandeAchatResponse cmd = new CommandeAchatResponse(
                commandeId, "CMD-AUTO", CommandeAchatStatut.RECEPTIONNEE,
                new FournisseurSummaryResponse(fournisseurId, "Fournisseur Chine"),
                new MagasinSummaryResponse(magasinId, "Magasin Central"),
                LocalDate.of(2026, 5, 15), List.of(), "2026-05-15 10:00:00"
        );
        FactureAchatResponse fac = new FactureAchatResponse(
                UUID.randomUUID(), "FAC-001", StatutFacture.NON_PAYEE,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                LocalDate.of(2026, 5, 15), null, commandeId
        );
        org.store.achat.application.dto.LigneCommandeAchatResponse ligne = new org.store.achat.application.dto.LigneCommandeAchatResponse(
                UUID.randomUUID(),
                new org.store.produit.application.dto.ProductSummaryResponse(UUID.randomUUID(), "Pneu", "PN-1"),
                new FournisseurSummaryResponse(fournisseurId, "Fournisseur Chine"),
                10, new BigDecimal("10.00"), new BigDecimal("15.00"), new BigDecimal("100.00")
        );
        org.store.achat.application.dto.AchatDetailsResponse details = new org.store.achat.application.dto.AchatDetailsResponse(cmd, fac, List.of(ligne));
        when(achatService.findDetailsById(commandeId)).thenReturn(details);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(AchatController.BASE_PATH + "/" + commandeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commande.reference").value("CMD-AUTO"))
                .andExpect(jsonPath("$.facture.numero").value("FAC-001"))
                .andExpect(jsonPath("$.lignes.length()").value(1))
                .andExpect(jsonPath("$.lignes[0].quantite").value(10))
                .andExpect(jsonPath("$.lignes[0].prixAchat").value(10.00))
                .andExpect(jsonPath("$.lignes[0].prixVente").value(15.00));
    }

    @Test
    void should_return_400_when_lignes_empty() throws Exception {
        AchatRequest body = new AchatRequest(magasinId, fournisseurId, LocalDate.of(2026, 5, 15),
                List.of(),
                new FactureAchatCreateRequest("FAC-001", LocalDate.of(2026, 5, 15), null));

        mockMvc.perform(post(AchatController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_facture_numero_blank() throws Exception {
        AchatRequest body = new AchatRequest(magasinId, fournisseurId, LocalDate.of(2026, 5, 15),
                List.of(new LigneAchatRequest(productFournisseurId, 100, new BigDecimal("10.00"), new BigDecimal("15.00"), null, null)),
                new FactureAchatCreateRequest("", LocalDate.of(2026, 5, 15), null));

        mockMvc.perform(post(AchatController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
