package org.store.entreprise.presentation;

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
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.entreprise.application.dto.EntrepriseRequest;
import org.store.entreprise.application.dto.EntrepriseResponse;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.security.application.dto.AccountRequest;
import org.store.security.application.dto.RegisterPropertyRequest;
import org.store.security.application.service.IRegisterPropertyService;
import org.store.users.application.dto.UtilisateurRequest;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EntrepriseControllerTest {

    private MockMvc mockMvc;
    private IEntrepriseService entrepriseService;
    private IRegisterPropertyService registerPropertyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID entrepriseId;

    @BeforeEach
    void setUp() {
        entrepriseService = mock(IEntrepriseService.class);
        registerPropertyService = mock(IRegisterPropertyService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new EntrepriseController(entrepriseService, registerPropertyService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        entrepriseId = UUID.randomUUID();
    }

    private EntrepriseResponse sample() {
        return new EntrepriseResponse(entrepriseId, "ACME", "ACME SARL", "N", "R", "Dakar", true, true, null);
    }

    private RegisterPropertyRequest validRegisterBody() {
        return new RegisterPropertyRequest(
                new AccountRequest("john.doe", "S3cretPwd!"),
                new UtilisateurRequest("Doe", "John", "john@example.com", "+221770000000", "Dakar"),
                new EntrepriseRequest("ACME", "ACME SARL", "NINEA-123", "RCCM-456", "Dakar"),
                new MagasinRequest("Magasin Centre", "Dakar Centre")
        );
    }

    @Test
    void should_return_201_with_entreprise_response_when_admin_creates() throws Exception {
        when(registerPropertyService.registerEntrepriseByAdmin(any(RegisterPropertyRequest.class))).thenReturn(sample());

        mockMvc.perform(post(EntrepriseController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(entrepriseId.toString()))
                .andExpect(jsonPath("$.sigle").value("ACME"))
                .andExpect(jsonPath("$.raisonSociale").value("ACME SARL"));
    }

    @Test
    void should_return_200_with_page_when_admin_lists() throws Exception {
        Page<EntrepriseResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(entrepriseService.findAll(any(org.store.entreprise.application.dto.EntrepriseFilter.class))).thenReturn(page);

        mockMvc.perform(get(EntrepriseController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(entrepriseId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_when_admin_gets_by_id() throws Exception {
        when(entrepriseService.findResponseById(eq(entrepriseId))).thenReturn(sample());

        mockMvc.perform(get(EntrepriseController.BASE_PATH + "/" + entrepriseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(entrepriseId.toString()));
    }

    @Test
    void should_return_200_when_admin_activates() throws Exception {
        when(entrepriseService.activate(eq(entrepriseId))).thenReturn(sample());

        mockMvc.perform(patch(EntrepriseController.BASE_PATH + "/" + entrepriseId + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(true));

        verify(entrepriseService).activate(entrepriseId);
    }

    @Test
    void should_return_200_when_admin_deactivates() throws Exception {
        EntrepriseResponse deactivated = new EntrepriseResponse(entrepriseId, "ACME", "ACME SARL",
                "N", "R", "Dakar", false, true, null);
        when(entrepriseService.deactivate(eq(entrepriseId))).thenReturn(deactivated);

        mockMvc.perform(patch(EntrepriseController.BASE_PATH + "/" + entrepriseId + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(false));

        verify(entrepriseService).deactivate(entrepriseId);
    }

    @Test
    void should_return_200_when_proprietaire_reads_his_own() throws Exception {
        when(entrepriseService.findCurrentUserEntreprise()).thenReturn(sample());

        mockMvc.perform(get(EntrepriseController.BASE_PATH + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(entrepriseId.toString()));
    }

    @Test
    void should_return_200_when_proprietaire_updates_his_own() throws Exception {
        EntrepriseRequest body = new EntrepriseRequest("NEW", "NEW SARL", "N2", "R2", "Adr2");
        EntrepriseResponse updated = new EntrepriseResponse(entrepriseId, "NEW", "NEW SARL",
                "N2", "R2", "Adr2", true, true, null);
        when(entrepriseService.updateCurrentUserEntreprise(any(EntrepriseRequest.class))).thenReturn(updated);

        mockMvc.perform(put(EntrepriseController.BASE_PATH + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sigle").value("NEW"));
    }
}
