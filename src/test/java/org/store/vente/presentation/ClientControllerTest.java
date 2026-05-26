package org.store.vente.presentation;

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
import org.store.vente.application.dto.ClientFilter;
import org.store.vente.application.dto.ClientRequest;
import org.store.vente.application.dto.ClientResponse;
import org.store.vente.application.service.IClientService;

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

class ClientControllerTest {

    private MockMvc mockMvc;
    private IClientService clientService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID clientId;
    private UUID magasinId;

    @BeforeEach
    void setUp() {
        clientService = mock(IClientService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ClientController(clientService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        clientId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
    }

    private ClientResponse sample() {
        return new ClientResponse(clientId, "Diallo", "Mamadou", "mamadou@example.com",
                "+221770000001", "Dakar");
    }

    @Test
    void should_return_201_when_created() throws Exception {
        ClientRequest body = new ClientRequest("Diallo", "Mamadou", "mamadou@example.com",
                "+221770000001", "Dakar", magasinId);
        when(clientService.create(any(ClientRequest.class))).thenReturn(sample());

        mockMvc.perform(post(ClientController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(clientId.toString()))
                .andExpect(jsonPath("$.nom").value("Diallo"))
                .andExpect(jsonPath("$.prenom").value("Mamadou"));
    }

    @Test
    void should_return_400_when_nom_blank() throws Exception {
        ClientRequest body = new ClientRequest("", null, null, magasinId);

        mockMvc.perform(post(ClientController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_magasinId_null() throws Exception {
        ClientRequest body = new ClientRequest("Diallo", null, null, null);

        mockMvc.perform(post(ClientController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_email_invalid() throws Exception {
        ClientRequest body = new ClientRequest("Diallo", null, "not-an-email", null, null, magasinId);

        mockMvc.perform(post(ClientController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_page_when_list() throws Exception {
        Page<ClientResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(clientService.findAllForCurrentUser(any(ClientFilter.class))).thenReturn(page);

        mockMvc.perform(get(ClientController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(clientId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_forward_nom_and_prenom_to_service_when_searching() throws Exception {
        Page<ClientResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(clientService.findAllForCurrentUser(any(ClientFilter.class))).thenReturn(page);

        mockMvc.perform(get(ClientController.BASE_PATH)
                        .param("nom", "Diallo")
                        .param("prenom", "Mama"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(clientId.toString()));

        org.mockito.ArgumentCaptor<ClientFilter> captor = org.mockito.ArgumentCaptor.forClass(ClientFilter.class);
        verify(clientService).findAllForCurrentUser(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().nom()).isEqualTo("Diallo");
        org.assertj.core.api.Assertions.assertThat(captor.getValue().prenom()).isEqualTo("Mama");
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(clientService.findResponseById(eq(clientId))).thenReturn(sample());

        mockMvc.perform(get(ClientController.BASE_PATH + "/" + clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clientId.toString()));
    }

    @Test
    void should_return_200_when_updated() throws Exception {
        ClientRequest body = new ClientRequest("Updated", null, null, "+221770000002", null, magasinId);
        ClientResponse updated = new ClientResponse(clientId, "Updated", null, null, null);
        when(clientService.update(eq(clientId), any(ClientRequest.class))).thenReturn(updated);

        mockMvc.perform(put(ClientController.BASE_PATH + "/" + clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Updated"));
    }

    @Test
    void should_return_204_when_deleted() throws Exception {
        mockMvc.perform(delete(ClientController.BASE_PATH + "/" + clientId))
                .andExpect(status().isNoContent());

        verify(clientService).delete(clientId);
    }
}
