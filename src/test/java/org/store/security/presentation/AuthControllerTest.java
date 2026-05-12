package org.store.security.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.entreprise.application.dto.EntrepriseRequest;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.security.application.dto.AccountRequest;
import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.LoginRequest;
import org.store.security.application.dto.RefreshTokenRequest;
import org.store.security.application.dto.RegisterPropertyRequest;
import org.store.security.application.service.ILoginService;
import org.store.security.application.service.IRefreshTokenService;
import org.store.security.application.service.IRegisterPropertyService;
import org.store.users.application.dto.UtilisateurRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private IRegisterPropertyService registerPropertyService;
    private ILoginService loginService;
    private IRefreshTokenService refreshTokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        registerPropertyService = mock(IRegisterPropertyService.class);
        loginService = mock(ILoginService.class);
        refreshTokenService = mock(IRefreshTokenService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(registerPropertyService, loginService, refreshTokenService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();
    }

    @Test
    void should_return_201_with_both_tokens_when_register_valid_payload() throws Exception {
        when(registerPropertyService.register(any(RegisterPropertyRequest.class)))
                .thenReturn(new AuthResponse("access.token", "refresh-uuid"));

        mockMvc.perform(post(AuthController.BASE_PATH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access.token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-uuid"));
    }

    @Test
    void should_return_400_when_register_payload_missing_required_fields() throws Exception {
        String invalidBody = """
                {
                  "account": {"username": "", "password": ""},
                  "utilisateur": {"nom": "", "prenom": "", "email": "not-an-email"},
                  "entreprise": {"sigle": "", "raisonSociale": ""},
                  "magasin": {"nom": "", "adresse": ""}
                }
                """;

        mockMvc.perform(post(AuthController.BASE_PATH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_register_payload_telephone_blank() throws Exception {
        String bodyWithoutTelephone = """
                {
                  "account": {"username": "john.doe", "password": "S3cretPwd!"},
                  "utilisateur": {"nom": "Doe", "prenom": "John", "email": "john@example.com", "telephone": "", "adresse": "Dakar"},
                  "entreprise": {"sigle": "ACME", "raisonSociale": "ACME SARL", "ninea": "NINEA-123", "rccm": "RCCM-456", "adresse": "Dakar"},
                  "magasin": {"nom": "Magasin Centre", "adresse": "Dakar Centre"}
                }
                """;

        mockMvc.perform(post(AuthController.BASE_PATH + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithoutTelephone))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_both_tokens_when_login_valid_credentials() throws Exception {
        LoginRequest body = new LoginRequest("john.doe", "S3cretPwd!");
        when(loginService.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("access.token", "refresh-uuid"));

        mockMvc.perform(post(AuthController.BASE_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-uuid"));
    }

    @Test
    void should_return_400_when_login_missing_credentials() throws Exception {
        String invalidBody = """
                {"username": "", "password": ""}
                """;

        mockMvc.perform(post(AuthController.BASE_PATH + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_new_access_token_when_refresh_valid() throws Exception {
        RefreshTokenRequest body = new RefreshTokenRequest("rt-value");
        when(refreshTokenService.refresh(eq("rt-value")))
                .thenReturn(new AuthResponse("new.access.token", "rt-value"));

        mockMvc.perform(post(AuthController.BASE_PATH + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("rt-value"));
    }

    @Test
    void should_return_400_when_refresh_token_blank() throws Exception {
        String invalidBody = """
                {"refreshToken": ""}
                """;

        mockMvc.perform(post(AuthController.BASE_PATH + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_204_when_logout_with_refresh_token() throws Exception {
        RefreshTokenRequest body = new RefreshTokenRequest("rt-value");

        mockMvc.perform(post(AuthController.BASE_PATH + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(refreshTokenService).revoke("rt-value");
    }

    @Test
    void should_return_400_when_logout_token_blank() throws Exception {
        String invalidBody = """
                {"refreshToken": ""}
                """;

        mockMvc.perform(post(AuthController.BASE_PATH + "/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    private RegisterPropertyRequest validRegisterBody() {
        return new RegisterPropertyRequest(
                new AccountRequest("john.doe", "S3cretPwd!"),
                new UtilisateurRequest("Doe", "John", "john@example.com", "+221700000000", "Dakar"),
                new EntrepriseRequest("ACME", "ACME SARL", "NINEA-123", "RCCM-456", "Dakar"),
                new MagasinRequest("Magasin Centre", "Dakar Centre")
        );
    }
}
