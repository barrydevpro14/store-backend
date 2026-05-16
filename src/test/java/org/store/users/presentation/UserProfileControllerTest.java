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
import org.store.security.application.dto.ChangePasswordRequest;
import org.store.users.application.dto.UserProfileResponse;
import org.store.users.application.dto.UserProfileUpdateRequest;
import org.store.users.application.service.IUserProfileService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileControllerTest {

    private MockMvc mockMvc;
    private IUserProfileService userProfileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID userId;
    private UUID magasinId;

    @BeforeEach
    void setUp() {
        userProfileService = mock(IUserProfileService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new UserProfileController(userProfileService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        userId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
    }

    private UserProfileResponse sample() {
        return new UserProfileResponse(userId, "Doe", "John", "john@example.com",
                "770000000", "Dakar", "john.emp", "VENDEUR", "EMPLOYE", magasinId);
    }

    @Test
    void should_return_200_when_get_current_profile() throws Exception {
        when(userProfileService.getCurrentProfile()).thenReturn(sample());

        mockMvc.perform(get(UserProfileController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.username").value("john.emp"))
                .andExpect(jsonPath("$.role").value("VENDEUR"))
                .andExpect(jsonPath("$.type").value("EMPLOYE"));
    }

    @Test
    void should_return_200_when_update_profile() throws Exception {
        UserProfileUpdateRequest body = new UserProfileUpdateRequest(
                "Doe", "Jane", "jane@example.com", "770000001", "Dakar");
        when(userProfileService.updateCurrentProfile(any(UserProfileUpdateRequest.class))).thenReturn(sample());

        mockMvc.perform(put(UserProfileController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void should_return_400_when_update_email_invalid() throws Exception {
        String invalidBody = """
                { "nom":"Doe","prenom":"Jane","email":"not-an-email","telephone":"770000001","adresse":"Dakar" }
                """;

        mockMvc.perform(put(UserProfileController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_204_when_change_password() throws Exception {
        ChangePasswordRequest body = new ChangePasswordRequest("oldPwd123", "newPwd1234");

        mockMvc.perform(post(UserProfileController.BASE_PATH + "/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        verify(userProfileService).changePassword(any(ChangePasswordRequest.class));
    }

    @Test
    void should_return_400_when_new_password_too_short() throws Exception {
        ChangePasswordRequest body = new ChangePasswordRequest("oldPwd123", "short");

        mockMvc.perform(post(UserProfileController.BASE_PATH + "/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_current_password_blank() throws Exception {
        ChangePasswordRequest body = new ChangePasswordRequest("", "newPwd1234");

        mockMvc.perform(post(UserProfileController.BASE_PATH + "/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
