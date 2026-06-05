package org.store.vente.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.dto.UserSummaryResponse;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.achat.application.dto.FournisseurSummaryResponse;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.vente.application.dto.AnnulationVenteRequest;
import org.store.vente.application.dto.AnnulationVenteResponse;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.application.dto.LigneCommandeVenteResponse;
import org.store.vente.application.dto.LigneVenteRequest;
import org.store.vente.application.dto.LigneVenteUpdateRequest;
import org.store.vente.application.dto.VenteDetailsResponse;
import org.store.vente.application.dto.VenteDraftResponse;
import org.store.vente.application.dto.VenteRequest;
import org.store.vente.application.dto.VenteResponse;
import org.store.vente.application.dto.VenteValidateRequest;
import org.store.vente.application.service.IVenteService;
import org.store.vente.domain.enums.CommandeVenteStatut;
import org.store.vente.domain.enums.MotifAnnulationVente;

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

class VenteControllerTest {

    private MockMvc mockMvc;
    private IVenteService venteService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID magasinId;
    private UUID productFournisseurId;
    private UUID commandeId;
    private UUID ligneId;

    @BeforeEach
    void setUp() {
        venteService = mock(IVenteService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new VenteController(venteService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        magasinId = UUID.randomUUID();
        productFournisseurId = UUID.randomUUID();
        commandeId = UUID.randomUUID();
        ligneId = UUID.randomUUID();
    }

    private VenteRequest validDraftBody() {
        return new VenteRequest(
                null,
                List.of(new LigneVenteRequest(productFournisseurId, 10, new BigDecimal("15.00")))
        );
    }

    private VenteValidateRequest validValidateBody() {
        return new VenteValidateRequest(LocalDate.now().plusDays(30), null);
    }

    private CommandeVenteResponse draftCommandeResponse() {
        return new CommandeVenteResponse(
                commandeId, "VTE-AUTO", CommandeVenteStatut.DRAFT,
                null,
                null,
                LocalDate.of(2026, 5, 18),
                BigDecimal.ZERO, BigDecimal.ZERO,
                null, "2026-05-18 10:00:00"
        );
    }

    private CommandeVenteResponse deliveredCommandeResponse() {
        return new CommandeVenteResponse(
                commandeId, "VTE-AUTO", CommandeVenteStatut.VALIDATE,
                null,
                new UserSummaryResponse(UUID.randomUUID(), "Diop Awa"),
                LocalDate.of(2026, 5, 18),
                new BigDecimal("150.00"), BigDecimal.ZERO,
                null, "2026-05-18 10:00:00"
        );
    }

    private FactureClientResponse sampleFacture() {
        return new FactureClientResponse(
                UUID.randomUUID(), "FAC-VTE-AUTO", StatutFacture.NON_PAYEE,
                new BigDecimal("150.00"), BigDecimal.ZERO, new BigDecimal("150.00"),
                LocalDate.of(2026, 5, 18), null, commandeId, null, null
        );
    }

    @Test
    void should_return_201_when_draft_created() throws Exception {
        when(venteService.create(any(VenteRequest.class))).thenReturn(new VenteDraftResponse(draftCommandeResponse(), List.of()));

        mockMvc.perform(post(VenteController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validDraftBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commande.reference").value("VTE-AUTO"))
                .andExpect(jsonPath("$.commande.statut").value("DRAFT"));
    }

    @Test
    void should_return_400_when_lignes_empty() throws Exception {
        VenteRequest body = new VenteRequest(null, List.of());

        mockMvc.perform(post(VenteController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_when_validate_sale() throws Exception {
        when(venteService.validate(eq(commandeId), any(VenteValidateRequest.class)))
                .thenReturn(new VenteResponse(deliveredCommandeResponse(), sampleFacture()));

        mockMvc.perform(post(VenteController.BASE_PATH + "/" + commandeId + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validValidateBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commande.statut").value("VALIDATE"))
                .andExpect(jsonPath("$.facture.numero").value("FAC-VTE-AUTO"));
    }

    @Test
    void should_return_400_when_validate_dateEcheance_missing() throws Exception {
        VenteValidateRequest body = new VenteValidateRequest(null, null);

        mockMvc.perform(post(VenteController.BASE_PATH + "/" + commandeId + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_when_get_sale_details() throws Exception {
        VenteDetailsResponse details = new VenteDetailsResponse(
                deliveredCommandeResponse(), sampleFacture(), List.of(), List.of()
        );
        when(venteService.findDetailsById(eq(commandeId))).thenReturn(details);

        mockMvc.perform(get(VenteController.BASE_PATH + "/" + commandeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commande.reference").value("VTE-AUTO"))
                .andExpect(jsonPath("$.facture.numero").value("FAC-VTE-AUTO"))
                .andExpect(jsonPath("$.lignes.length()").value(0));
    }

    @Test
    void should_return_200_when_update_ligne() throws Exception {
        LigneCommandeVenteResponse updated = new LigneCommandeVenteResponse(
                ligneId,
                new ProductSummaryResponse(UUID.randomUUID(), "Pneu", "PN-1", null),
                null,
                20, new BigDecimal("18.00"), new BigDecimal("360.00")
        );
        when(venteService.updateLigne(eq(commandeId), eq(ligneId), any(LigneVenteUpdateRequest.class)))
                .thenReturn(updated);

        LigneVenteUpdateRequest body = new LigneVenteUpdateRequest(20, new BigDecimal("18.00"));

        mockMvc.perform(put(VenteController.BASE_PATH + "/orders/" + commandeId + "/lignes/" + ligneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantite").value(20))
                .andExpect(jsonPath("$.prixUnitaire").value(18.00));
    }

    @Test
    void should_return_400_when_update_ligne_quantite_zero() throws Exception {
        LigneVenteUpdateRequest body = new LigneVenteUpdateRequest(0, new BigDecimal("18.00"));

        mockMvc.perform(put(VenteController.BASE_PATH + "/orders/" + commandeId + "/lignes/" + ligneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_204_when_delete_ligne() throws Exception {
        mockMvc.perform(delete(VenteController.BASE_PATH + "/orders/" + commandeId + "/lignes/" + ligneId))
                .andExpect(status().isNoContent());

        verify(venteService).deleteLigne(commandeId, ligneId);
    }

    @Test
    void should_return_200_when_cancel_sale() throws Exception {
        AnnulationVenteResponse cancelResponse = new AnnulationVenteResponse(
                commandeId, "VTE-AUTO",
                CommandeVenteStatut.CANCEL,
                MotifAnnulationVente.ERREUR_SAISIE,
                "Saisie incorrecte",
                "2026-05-18 14:30:00",
                8, 1
        );
        when(venteService.cancel(eq(commandeId), any(AnnulationVenteRequest.class))).thenReturn(cancelResponse);

        AnnulationVenteRequest body = new AnnulationVenteRequest("ERREUR_SAISIE", "Saisie incorrecte");

        mockMvc.perform(post(VenteController.BASE_PATH + "/" + commandeId + "/annuler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commandeId").value(commandeId.toString()))
                .andExpect(jsonPath("$.statut").value("CANCEL"))
                .andExpect(jsonPath("$.motif").value("ERREUR_SAISIE"))
                .andExpect(jsonPath("$.totalQuantiteReinjectee").value(8))
                .andExpect(jsonPath("$.nombreMouvementsCrees").value(1));
    }

    @Test
    void should_return_400_when_cancel_motif_invalid() throws Exception {
        AnnulationVenteRequest body = new AnnulationVenteRequest("MOTIF_INCONNU", null);

        mockMvc.perform(post(VenteController.BASE_PATH + "/" + commandeId + "/annuler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_cancel_motif_blank() throws Exception {
        AnnulationVenteRequest body = new AnnulationVenteRequest("", null);

        mockMvc.perform(post(VenteController.BASE_PATH + "/" + commandeId + "/annuler")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
