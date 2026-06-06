package org.store.users.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.security.application.dto.AccountRequest;
import org.store.security.application.dto.ResetPasswordRequest;
import org.store.users.application.dto.EmployeFilter;
import org.store.users.application.dto.EmployeRequest;
import org.store.users.application.dto.EmployeResponse;
import org.store.users.application.dto.EmployeUpdateRequest;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.application.service.IEmployeService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmployeControllerTest {

    private MockMvc mockMvc;
    private IEmployeService employeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        employeService = mock(IEmployeService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        EmployeController controller = new EmployeController(employeService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();
    }

    private EmployeRequest validBody(UUID magasinId, String role) {
        return new EmployeRequest(
                "john.emp",
                new UtilisateurRequest("Doe", "John", "john@example.com", "+221770000000", "Dakar"),
                role,
                magasinId
        );
    }

    @Test
    void should_return_201_with_employe_response_when_payload_valid() throws Exception {
        UUID magasinId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();
        EmployeResponse response = new EmployeResponse(createdId, "Doe", "John",
                "john@example.com", "+221770000000", "Dakar", "john.emp", "MANAGER", magasinId, true);

        when(employeService.create(any(EmployeRequest.class))).thenReturn(response);

        mockMvc.perform(post(EmployeController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody(magasinId, "MANAGER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(createdId.toString()))
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.magasinId").value(magasinId.toString()));
    }

    @Test
    void should_return_400_when_telephone_invalid() throws Exception {
        String invalidBody = """
                {
                  "account": {"username": "john.emp", "password": "S3cretPwd!"},
                  "utilisateur": {"nom": "Doe", "prenom": "John", "email": "john@example.com", "telephone": "770000000", "adresse": "Dakar"},
                  "role": "MANAGER",
                  "magasinId": "%s"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post(EmployeController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    private EmployeResponse sampleResponse(UUID id, UUID magasinId) {
        return new EmployeResponse(id, "Doe", "John", "john@example.com", "+221770000000", "Dakar",
                "john.emp", "SELLER", magasinId, true);
    }

    @Test
    void should_return_200_when_list_employees() throws Exception {
        UUID magasinId = UUID.randomUUID();
        Page<EmployeResponse> page = new PageImpl<>(List.of(sampleResponse(UUID.randomUUID(), magasinId)),
                PageRequest.of(0, 10), 1);
        when(employeService.findAllByCurrentEntreprise(any(EmployeFilter.class))).thenReturn(page);

        mockMvc.perform(get(EmployeController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].role").value("SELLER"))
                .andExpect(jsonPath("$.content[0].actif").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        UUID id = UUID.randomUUID();
        when(employeService.findResponseById(id)).thenReturn(sampleResponse(id, UUID.randomUUID()));

        mockMvc.perform(get(EmployeController.BASE_PATH + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void should_return_200_when_update_employee() throws Exception {
        UUID id = UUID.randomUUID();
        UUID magasinId = UUID.randomUUID();
        EmployeUpdateRequest body = new EmployeUpdateRequest("Doe", "Jane", "jane@example.com",
                "+221770000001", "Dakar", "SELLER", magasinId);
        when(employeService.update(eq(id), any(EmployeUpdateRequest.class)))
                .thenReturn(sampleResponse(id, magasinId));

        mockMvc.perform(put(EmployeController.BASE_PATH + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_400_when_update_email_invalid() throws Exception {
        UUID id = UUID.randomUUID();
        String invalidBody = """
                {
                  "nom": "Doe",
                  "prenom": "Jane",
                  "email": "not-an-email",
                  "telephone": "+221770000001",
                  "adresse": "Dakar",
                  "role": "SELLER",
                  "magasinId": "%s"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(put(EmployeController.BASE_PATH + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_204_when_deactivate() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete(EmployeController.BASE_PATH + "/" + id))
                .andExpect(status().isNoContent());

        verify(employeService).deactivate(id);
    }

    @Test
    void should_return_204_when_reset_password() throws Exception {
        UUID id = UUID.randomUUID();
        ResetPasswordRequest body = new ResetPasswordRequest("brandnewP@ss");

        mockMvc.perform(post(EmployeController.BASE_PATH + "/" + id + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        verify(employeService).resetPassword(eq(id), any(ResetPasswordRequest.class));
    }

    @Test
    void should_return_400_when_reset_password_too_short() throws Exception {
        UUID id = UUID.randomUUID();
        ResetPasswordRequest body = new ResetPasswordRequest("short");

        mockMvc.perform(post(EmployeController.BASE_PATH + "/" + id + "/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_204_when_activate() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch(EmployeController.BASE_PATH + "/" + id + "/activate"))
                .andExpect(status().isNoContent());

        verify(employeService).activate(id);
    }

    @Test
    void should_return_400_when_role_blank() throws Exception {
        String invalidBody = """
                {
                  "account": {"username": "john.emp", "password": "S3cretPwd!"},
                  "utilisateur": {"nom": "Doe", "prenom": "John", "email": "john@example.com", "telephone": "+221770000000", "adresse": "Dakar"},
                  "role": "",
                  "magasinId": "%s"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post(EmployeController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }
}
