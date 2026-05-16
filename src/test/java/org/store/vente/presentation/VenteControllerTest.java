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
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.application.dto.LigneVenteRequest;
import org.store.vente.application.dto.VenteDetailsResponse;
import org.store.vente.application.dto.VenteRequest;
import org.store.vente.application.dto.VenteResponse;
import org.store.vente.application.service.IVenteService;
import org.store.vente.domain.enums.CommandeVenteStatut;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VenteControllerTest {

    private MockMvc mockMvc;
    private IVenteService venteService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID magasinId;
    private UUID productFournisseurId;

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
    }

    private VenteRequest validBody() {
        return new VenteRequest(
                null,
                LocalDate.of(2026, 5, 16),
                LocalDate.of(2026, 5, 16),
                List.of(new LigneVenteRequest(productFournisseurId, 10, new BigDecimal("15.00"))),
                null
        );
    }

    private VenteResponse sample() {
        UUID commandeId = UUID.randomUUID();
        CommandeVenteResponse cmd = new CommandeVenteResponse(
                commandeId, "VTE-AUTO", CommandeVenteStatut.DELIVERED,
                null,
                new MagasinSummaryResponse(magasinId, "Magasin Central"),
                new UserSummaryResponse(UUID.randomUUID(), "Diop Awa"),
                LocalDate.of(2026, 5, 16),
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                "2026-05-16 10:00:00"
        );
        FactureClientResponse fac = new FactureClientResponse(
                UUID.randomUUID(), "FAC-VTE-AUTO", StatutFacture.NON_PAYEE,
                new BigDecimal("150.00"), BigDecimal.ZERO, new BigDecimal("150.00"),
                LocalDate.of(2026, 5, 16), null, commandeId
        );
        return new VenteResponse(cmd, fac);
    }

    @Test
    void should_return_201_when_sale_created() throws Exception {
        when(venteService.create(any(VenteRequest.class))).thenReturn(sample());

        mockMvc.perform(post(VenteController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commande.reference").value("VTE-AUTO"))
                .andExpect(jsonPath("$.commande.user.nomComplet").value("Diop Awa"))
                .andExpect(jsonPath("$.facture.numero").value("FAC-VTE-AUTO"));
    }

    @Test
    void should_return_400_when_lignes_empty() throws Exception {
        VenteRequest body = new VenteRequest(null, LocalDate.of(2026, 5, 16), LocalDate.of(2026, 5, 16), List.of(), null);

        mockMvc.perform(post(VenteController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_when_get_sale_details() throws Exception {
        UUID commandeId = UUID.randomUUID();
        CommandeVenteResponse cmd = new CommandeVenteResponse(
                commandeId, "VTE-AUTO", CommandeVenteStatut.DELIVERED,
                null,
                new MagasinSummaryResponse(magasinId, "Magasin Central"),
                new UserSummaryResponse(UUID.randomUUID(), "Diop Awa"),
                LocalDate.of(2026, 5, 16),
                new BigDecimal("150.00"),
                BigDecimal.ZERO,
                "2026-05-16 10:00:00"
        );
        FactureClientResponse fac = new FactureClientResponse(
                UUID.randomUUID(), "FAC-VTE-AUTO", StatutFacture.NON_PAYEE,
                new BigDecimal("150.00"), BigDecimal.ZERO, new BigDecimal("150.00"),
                LocalDate.of(2026, 5, 16), null, commandeId
        );
        VenteDetailsResponse details = new VenteDetailsResponse(cmd, fac, List.of(), List.of());
        when(venteService.findDetailsById(eq(commandeId))).thenReturn(details);

        mockMvc.perform(get(VenteController.BASE_PATH + "/" + commandeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commande.reference").value("VTE-AUTO"))
                .andExpect(jsonPath("$.commande.user.nomComplet").value("Diop Awa"))
                .andExpect(jsonPath("$.facture.numero").value("FAC-VTE-AUTO"))
                .andExpect(jsonPath("$.lignes.length()").value(0))
                .andExpect(jsonPath("$.paiements.length()").value(0));
    }
}
