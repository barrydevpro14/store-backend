package org.store.achat.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.store.achat.application.dto.FournisseurRequest;
import org.store.achat.application.dto.FournisseurResponse;
import org.store.achat.application.service.IFournisseurService;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;

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

class FournisseurControllerTest {

    private MockMvc mockMvc;
    private IFournisseurService fournisseurService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID fournisseurId;
    private UUID entrepriseId;

    @BeforeEach
    void setUp() {
        fournisseurService = mock(IFournisseurService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new FournisseurController(fournisseurService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        fournisseurId = UUID.randomUUID();
        entrepriseId = UUID.randomUUID();
    }

    private FournisseurResponse sample() {
        return new FournisseurResponse(fournisseurId, "Pneus Maroc SARL", null,
                "contact@pneus-maroc.ma", "+221770000000", "Casablanca", "FRN-001", "Maroc", entrepriseId);
    }

    @Test
    void should_return_201_when_created() throws Exception {
        FournisseurRequest body = new FournisseurRequest(
                "Pneus Maroc SARL", null, "contact@pneus-maroc.ma", "+221770000000",
                "Casablanca", "FRN-001", "Maroc"
        );
        when(fournisseurService.create(any(FournisseurRequest.class))).thenReturn(sample());

        mockMvc.perform(post(FournisseurController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(fournisseurId.toString()))
                .andExpect(jsonPath("$.nom").value("Pneus Maroc SARL"))
                .andExpect(jsonPath("$.reference").value("FRN-001"))
                .andExpect(jsonPath("$.entrepriseId").value(entrepriseId.toString()));
    }

    @Test
    void should_return_400_when_nom_blank() throws Exception {
        FournisseurRequest body = new FournisseurRequest("", null, null, null, null, null, null);

        mockMvc.perform(post(FournisseurController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_email_invalid() throws Exception {
        FournisseurRequest body = new FournisseurRequest("Nom OK", null, "not-an-email", null, null, null, null);

        mockMvc.perform(post(FournisseurController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_page_when_list() throws Exception {
        Page<FournisseurResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(fournisseurService.findAllByCurrentEntreprise(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(FournisseurController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(fournisseurId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(fournisseurService.findResponseById(eq(fournisseurId))).thenReturn(sample());

        mockMvc.perform(get(FournisseurController.BASE_PATH + "/" + fournisseurId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(fournisseurId.toString()));
    }

    @Test
    void should_return_200_when_updated() throws Exception {
        FournisseurRequest body = new FournisseurRequest(
                "Updated", null, null, null, null, "FRN-001", null
        );
        FournisseurResponse updated = new FournisseurResponse(fournisseurId, "Updated", null,
                null, null, null, "FRN-001", null, entrepriseId);
        when(fournisseurService.update(eq(fournisseurId), any(FournisseurRequest.class))).thenReturn(updated);

        mockMvc.perform(put(FournisseurController.BASE_PATH + "/" + fournisseurId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Updated"));
    }

    @Test
    void should_return_204_when_deleted() throws Exception {
        mockMvc.perform(delete(FournisseurController.BASE_PATH + "/" + fournisseurId))
                .andExpect(status().isNoContent());

        verify(fournisseurService).delete(fournisseurId);
    }
}
