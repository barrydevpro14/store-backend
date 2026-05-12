package org.store.users.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.security.application.dto.AccountRequest;
import org.store.users.application.dto.EmployeRequest;
import org.store.users.application.dto.EmployeResponse;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.application.service.IEmployeService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                new AccountRequest("john.emp", "S3cretPwd!"),
                new UtilisateurRequest("Doe", "John", "john@example.com", "770000000", "Dakar"),
                role,
                magasinId
        );
    }

    @Test
    void should_return_201_with_employe_response_when_payload_valid() throws Exception {
        UUID magasinId = UUID.randomUUID();
        UUID createdId = UUID.randomUUID();
        EmployeResponse response = new EmployeResponse(createdId, "Doe", "John",
                "john@example.com", "770000000", "Dakar", "john.emp", "MANAGER", magasinId);

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
                  "utilisateur": {"nom": "Doe", "prenom": "John", "email": "john@example.com", "telephone": "+221770000000", "adresse": "Dakar"},
                  "role": "MANAGER",
                  "magasinId": "%s"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post(EmployeController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_role_blank() throws Exception {
        String invalidBody = """
                {
                  "account": {"username": "john.emp", "password": "S3cretPwd!"},
                  "utilisateur": {"nom": "Doe", "prenom": "John", "email": "john@example.com", "telephone": "770000000", "adresse": "Dakar"},
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
