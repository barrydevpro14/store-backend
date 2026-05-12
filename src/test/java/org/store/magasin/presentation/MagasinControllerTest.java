package org.store.magasin.presentation;

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
import org.store.magasin.application.dto.MagasinRequest;
import org.store.magasin.application.dto.MagasinResponse;
import org.store.magasin.application.service.IMagasinService;

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

class MagasinControllerTest {

    private MockMvc mockMvc;
    private IMagasinService magasinService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID magasinId;
    private UUID entrepriseId;

    @BeforeEach
    void setUp() {
        magasinService = mock(IMagasinService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new MagasinController(magasinService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        magasinId = UUID.randomUUID();
        entrepriseId = UUID.randomUUID();
    }

    private MagasinResponse sample() {
        return new MagasinResponse(magasinId, "Magasin Centre", "Dakar Centre", true, entrepriseId);
    }

    @Test
    void should_return_201_with_created_magasin() throws Exception {
        MagasinRequest body = new MagasinRequest("Magasin Centre", "Dakar Centre");
        when(magasinService.create(any(MagasinRequest.class))).thenReturn(sample());

        mockMvc.perform(post(MagasinController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(magasinId.toString()))
                .andExpect(jsonPath("$.entrepriseId").value(entrepriseId.toString()))
                .andExpect(jsonPath("$.actif").value(true));
    }

    @Test
    void should_return_400_when_create_payload_invalid() throws Exception {
        String invalid = """
                {"nom": "", "adresse": ""}
                """;

        mockMvc.perform(post(MagasinController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_page_when_list() throws Exception {
        Page<MagasinResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(magasinService.findAllByCurrentEntreprise(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(MagasinController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(magasinId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(magasinService.findResponseById(eq(magasinId))).thenReturn(sample());

        mockMvc.perform(get(MagasinController.BASE_PATH + "/" + magasinId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(magasinId.toString()));
    }

    @Test
    void should_return_200_when_update() throws Exception {
        MagasinRequest body = new MagasinRequest("Renommé", "Nouvelle adresse");
        MagasinResponse updated = new MagasinResponse(magasinId, "Renommé", "Nouvelle adresse", true, entrepriseId);
        when(magasinService.update(eq(magasinId), any(MagasinRequest.class))).thenReturn(updated);

        mockMvc.perform(put(MagasinController.BASE_PATH + "/" + magasinId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Renommé"));
    }

    @Test
    void should_return_200_when_activate() throws Exception {
        when(magasinService.activate(eq(magasinId)))
                .thenReturn(new MagasinResponse(magasinId, "M", "A", true, entrepriseId));

        mockMvc.perform(patch(MagasinController.BASE_PATH + "/" + magasinId + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(true));

        verify(magasinService).activate(magasinId);
    }

    @Test
    void should_return_200_when_deactivate() throws Exception {
        when(magasinService.deactivate(eq(magasinId)))
                .thenReturn(new MagasinResponse(magasinId, "M", "A", false, entrepriseId));

        mockMvc.perform(patch(MagasinController.BASE_PATH + "/" + magasinId + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(false));

        verify(magasinService).deactivate(magasinId);
    }
}
