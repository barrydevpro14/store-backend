package org.store.depense.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.depense.application.dto.CategoryDepenseSummaryResponse;
import org.store.depense.application.dto.DepenseFilter;
import org.store.depense.application.dto.DepenseRequest;
import org.store.depense.application.dto.DepenseResponse;
import org.store.depense.application.dto.DepenseTotalResponse;
import org.store.depense.application.service.IDepenseService;
import org.store.magasin.application.dto.MagasinSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DepenseControllerTest {

    private MockMvc mockMvc;
    private IDepenseService depenseService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private UUID magasinId;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        depenseService = mock(IDepenseService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new DepenseController(depenseService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        magasinId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
    }

    private DepenseRequest validBody() {
        return new DepenseRequest(magasinId, categoryId, "Loyer mai", "desc",
                LocalDate.of(2026, 5, 1), new BigDecimal("250000.00"), java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));
    }

    private DepenseResponse sample() {
        return new DepenseResponse(
                UUID.randomUUID(),
                new MagasinSummaryResponse(magasinId, "Magasin Central"),
                new CategoryDepenseSummaryResponse(categoryId, "Loyer"),
                "Loyer mai", "desc",
                "2026-05-01",
                new BigDecimal("250000.00"), new MoyenPaiementResponse(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), "Espèces", true),
                "2026-05-01 10:00:00"
        );
    }

    @Test
    void should_return_201_when_depense_created() throws Exception {
        when(depenseService.create(any(DepenseRequest.class))).thenReturn(sample());

        mockMvc.perform(post(DepenseController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.libelle").value("Loyer mai"))
                .andExpect(jsonPath("$.montant").value(250000.00))
                .andExpect(jsonPath("$.modePaiement.libelle").value("Espèces"));
    }

    @Test
    void should_return_400_when_libelle_blank() throws Exception {
        DepenseRequest body = new DepenseRequest(magasinId, categoryId, "", null,
                LocalDate.now(), new BigDecimal("100.00"), java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));

        mockMvc.perform(post(DepenseController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_total() throws Exception {
        DepenseTotalResponse total = new DepenseTotalResponse(magasinId, new BigDecimal("750000.00"), 3L);
        when(depenseService.computeTotal(any(DepenseFilter.class))).thenReturn(total);

        mockMvc.perform(get(DepenseController.BASE_PATH + "/total")
                        .param("magasinId", magasinId.toString())
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantTotal").value(750000.00))
                .andExpect(jsonPath("$.nombreDepenses").value(3));
    }

    @Test
    void should_return_200_with_listing() throws Exception {
        Page<DepenseResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(depenseService.findAllByCurrentEntreprise(any(DepenseFilter.class))).thenReturn(page);

        mockMvc.perform(get(DepenseController.BASE_PATH).param("magasinId", magasinId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].libelle").value("Loyer mai"));
    }
}
