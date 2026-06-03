package org.store.achat.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.achat.application.dto.AchatDetailsResponse;
import org.store.achat.application.dto.AchatDraftResponse;
import org.store.achat.application.dto.AchatReceiveRequest;
import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatResponse;
import org.store.achat.application.dto.AnnulationAchatRequest;
import org.store.achat.application.dto.AnnulationAchatResponse;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.application.dto.FactureAchatCreateRequest;
import org.store.achat.application.dto.FactureAchatResponse;
import org.store.achat.application.dto.FournisseurSummaryResponse;
import org.store.achat.application.dto.LigneAchatRequest;
import org.store.achat.application.dto.LigneAchatUpdateRequest;
import org.store.achat.application.dto.LigneCommandeAchatResponse;
import org.store.achat.application.service.IAchatService;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.enums.MotifAnnulationAchat;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.produit.application.dto.ProductSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AchatControllerTest {

    private MockMvc mockMvc;
    private IAchatService achatService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID magasinId;
    private UUID fournisseurId;
    private UUID productFournisseurId;
    private UUID commandeId;
    private UUID ligneId;

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
        commandeId = UUID.randomUUID();
        ligneId = UUID.randomUUID();
    }

    private AchatRequest validDraftBody() {
        return new AchatRequest(magasinId, fournisseurId, LocalDate.of(2026, 5, 15),
                List.of(new LigneAchatRequest(productFournisseurId, 100, new BigDecimal("10.00"), new BigDecimal("15.00"), "LOT-001", null)));
    }

    private AchatReceiveRequest validReceiveBody() {
        return new AchatReceiveRequest(
                new FactureAchatCreateRequest("FAC-001", LocalDate.of(2026, 5, 15), LocalDate.of(2026, 6, 15)),
                null);
    }

    private CommandeAchatResponse draftCommandeResponse() {
        return new CommandeAchatResponse(
                commandeId, "CMD-AUTO", CommandeAchatStatut.DRAFT,
                new FournisseurSummaryResponse(fournisseurId, "Fournisseur Chine"),
                new MagasinSummaryResponse(magasinId, "Magasin Central"),
                LocalDate.of(2026, 5, 15), List.of(), null, "2026-05-15 10:00:00"
        );
    }

    private CommandeAchatResponse receptionneeCommandeResponse() {
        return new CommandeAchatResponse(
                commandeId, "CMD-AUTO", CommandeAchatStatut.RECEPTIONNEE,
                new FournisseurSummaryResponse(fournisseurId, "Fournisseur Chine"),
                new MagasinSummaryResponse(magasinId, "Magasin Central"),
                LocalDate.of(2026, 5, 15), List.of(), null, "2026-05-15 10:00:00"
        );
    }

    private FactureAchatResponse sampleFacture() {
        return new FactureAchatResponse(
                UUID.randomUUID(), "FAC-001", StatutFacture.NON_PAYEE,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                LocalDate.of(2026, 5, 15), null, commandeId
        );
    }

    @Test
    void should_return_201_when_draft_created() throws Exception {
        when(achatService.create(any(AchatRequest.class))).thenReturn(new AchatDraftResponse(draftCommandeResponse()));

        mockMvc.perform(post(AchatController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDraftBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commande.reference").value("CMD-AUTO"))
                .andExpect(jsonPath("$.commande.statut").value("DRAFT"));
    }

    @Test
    void should_return_400_when_lignes_empty() throws Exception {
        AchatRequest body = new AchatRequest(magasinId, fournisseurId, LocalDate.of(2026, 5, 15), List.of());

        mockMvc.perform(post(AchatController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_when_receive_purchase() throws Exception {
        when(achatService.receive(eq(commandeId), any(AchatReceiveRequest.class)))
                .thenReturn(new AchatResponse(receptionneeCommandeResponse(), sampleFacture()));

        mockMvc.perform(post(AchatController.BASE_PATH + "/" + commandeId + "/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReceiveBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commande.statut").value("RECEPTIONNEE"))
                .andExpect(jsonPath("$.facture.numero").value("FAC-001"));
    }

    @Test
    void should_return_200_when_receive_with_blank_numero() throws Exception {
        // numero blank → backend auto-generates; controller-level validation must let it through.
        when(achatService.receive(eq(commandeId), any(AchatReceiveRequest.class)))
                .thenReturn(new AchatResponse(receptionneeCommandeResponse(), sampleFacture()));

        AchatReceiveRequest body = new AchatReceiveRequest(
                new FactureAchatCreateRequest("", LocalDate.of(2026, 5, 15), LocalDate.of(2026, 6, 15)),
                null);

        mockMvc.perform(post(AchatController.BASE_PATH + "/" + commandeId + "/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_400_when_receive_facture_dateEcheance_missing() throws Exception {
        AchatReceiveRequest body = new AchatReceiveRequest(
                new FactureAchatCreateRequest("FAC-001", LocalDate.of(2026, 5, 15), null),
                null);

        mockMvc.perform(post(AchatController.BASE_PATH + "/" + commandeId + "/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_when_get_purchase_details() throws Exception {
        LigneCommandeAchatResponse ligne = new LigneCommandeAchatResponse(
                UUID.randomUUID(),
                new ProductSummaryResponse(UUID.randomUUID(), "Pneu", "PN-1", "Auto"),
                new FournisseurSummaryResponse(fournisseurId, "Fournisseur Chine"),
                10, 0, new BigDecimal("10.00"), new BigDecimal("15.00"), new BigDecimal("100.00"),
                "LOT-001", null
        );
        AchatDetailsResponse details = new AchatDetailsResponse(receptionneeCommandeResponse(), sampleFacture(), List.of(ligne));
        when(achatService.findDetailsById(commandeId)).thenReturn(details);

        mockMvc.perform(get(AchatController.BASE_PATH + "/" + commandeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commande.reference").value("CMD-AUTO"))
                .andExpect(jsonPath("$.facture.numero").value("FAC-001"))
                .andExpect(jsonPath("$.lignes.length()").value(1))
                .andExpect(jsonPath("$.lignes[0].quantite").value(10));
    }

    @Test
    void should_return_200_when_update_ligne() throws Exception {
        LigneCommandeAchatResponse updated = new LigneCommandeAchatResponse(
                ligneId,
                new ProductSummaryResponse(UUID.randomUUID(), "Pneu", "PN-1", "Auto"),
                new FournisseurSummaryResponse(fournisseurId, "Fournisseur Chine"),
                200, 0, new BigDecimal("12.00"), new BigDecimal("18.00"), new BigDecimal("2400.00"),
                "LOT-002", null
        );
        when(achatService.updateLigne(eq(commandeId), eq(ligneId), any(LigneAchatUpdateRequest.class)))
                .thenReturn(updated);

        LigneAchatUpdateRequest body = new LigneAchatUpdateRequest(200, new BigDecimal("12.00"), new BigDecimal("18.00"), "LOT-002", null);

        mockMvc.perform(put(AchatController.BASE_PATH + "/orders/" + commandeId + "/lignes/" + ligneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantite").value(200))
                .andExpect(jsonPath("$.prixAchat").value(12.00))
                .andExpect(jsonPath("$.prixVente").value(18.00));
    }

    @Test
    void should_return_400_when_update_ligne_quantite_zero() throws Exception {
        LigneAchatUpdateRequest body = new LigneAchatUpdateRequest(0, new BigDecimal("12.00"), new BigDecimal("18.00"), null, null);

        mockMvc.perform(put(AchatController.BASE_PATH + "/orders/" + commandeId + "/lignes/" + ligneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_204_when_delete_ligne() throws Exception {
        mockMvc.perform(delete(AchatController.BASE_PATH + "/orders/" + commandeId + "/lignes/" + ligneId))
                .andExpect(status().isNoContent());

        verify(achatService).deleteLigne(commandeId, ligneId);
    }

    @Test
    void should_return_204_when_delete_draft() throws Exception {
        mockMvc.perform(delete(AchatController.BASE_PATH + "/" + commandeId))
                .andExpect(status().isNoContent());

        verify(achatService).deleteDraft(commandeId);
    }

    @Test
    void should_return_200_when_cancel_purchase() throws Exception {
        AnnulationAchatResponse response = new AnnulationAchatResponse(
                commandeId, "CMD-AUTO", CommandeAchatStatut.ANNULEE,
                MotifAnnulationAchat.ERREUR_SAISIE, "Saisie erronée", "2026-05-18 10:00:00",
                100, 1
        );
        when(achatService.cancel(eq(commandeId), any(AnnulationAchatRequest.class))).thenReturn(response);

        AnnulationAchatRequest body = new AnnulationAchatRequest("ERREUR_SAISIE", "Saisie erronée");

        mockMvc.perform(post(AchatController.BASE_PATH + "/" + commandeId + "/annuler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULEE"))
                .andExpect(jsonPath("$.motif").value("ERREUR_SAISIE"))
                .andExpect(jsonPath("$.totalQuantiteRetiree").value(100))
                .andExpect(jsonPath("$.nombreMouvementsCrees").value(1));
    }

    @Test
    void should_return_400_when_cancel_motif_invalid() throws Exception {
        AnnulationAchatRequest body = new AnnulationAchatRequest("MOTIF_INEXISTANT", null);

        mockMvc.perform(post(AchatController.BASE_PATH + "/" + commandeId + "/annuler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_cancel_motif_blank() throws Exception {
        AnnulationAchatRequest body = new AnnulationAchatRequest("", null);

        mockMvc.perform(post(AchatController.BASE_PATH + "/" + commandeId + "/annuler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
