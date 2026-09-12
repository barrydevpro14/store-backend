package org.store.produit.presentation;

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
import org.store.produit.application.dto.UniteMesureFilter;
import org.store.produit.application.dto.UniteMesureRequest;
import org.store.produit.application.dto.UniteMesureResponse;
import org.store.produit.application.dto.UniteMesureSummaryResponse;
import org.store.produit.application.service.IUniteMesureService;

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

class UniteMesureControllerTest {

    private MockMvc mockMvc;
    private IUniteMesureService uniteMesureService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID uniteId;

    @BeforeEach
    void setUp() {
        uniteMesureService = mock(IUniteMesureService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new UniteMesureController(uniteMesureService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        uniteId = UUID.randomUUID();
    }

    private UniteMesureResponse sample() {
        return new UniteMesureResponse(uniteId, "KG", "Kilogramme", "kg");
    }

    @Test
    void should_return_200_with_summaries_when_list_all() throws Exception {
        UniteMesureSummaryResponse summary = new UniteMesureSummaryResponse(uniteId, "KG", "Kilogramme", "kg");
        when(uniteMesureService.listAll()).thenReturn(List.of(summary));

        mockMvc.perform(get(UniteMesureController.BASE_PATH + "/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(uniteId.toString()))
                .andExpect(jsonPath("$[0].code").value("KG"));
    }

    @Test
    void should_return_201_when_created() throws Exception {
        UniteMesureRequest body = new UniteMesureRequest("KG", "Kilogramme", "kg");
        when(uniteMesureService.create(any(UniteMesureRequest.class))).thenReturn(sample());

        mockMvc.perform(post(UniteMesureController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(uniteId.toString()))
                .andExpect(jsonPath("$.code").value("KG"));
    }

    @Test
    void should_return_400_when_libelle_blank() throws Exception {
        UniteMesureRequest body = new UniteMesureRequest("KG", "", "kg");

        mockMvc.perform(post(UniteMesureController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_page_when_list() throws Exception {
        Page<UniteMesureResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(uniteMesureService.findAll(any(UniteMesureFilter.class))).thenReturn(page);

        mockMvc.perform(get(UniteMesureController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(uniteId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(uniteMesureService.findResponseById(eq(uniteId))).thenReturn(sample());

        mockMvc.perform(get(UniteMesureController.BASE_PATH + "/" + uniteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(uniteId.toString()))
                .andExpect(jsonPath("$.code").value("KG"));
    }

    @Test
    void should_return_200_when_updated() throws Exception {
        UniteMesureRequest body = new UniteMesureRequest("KG", "Kilo", "Kg");
        UniteMesureResponse updated = new UniteMesureResponse(uniteId, "KG", "Kilo", "Kg");
        when(uniteMesureService.update(eq(uniteId), any(UniteMesureRequest.class))).thenReturn(updated);

        mockMvc.perform(put(UniteMesureController.BASE_PATH + "/" + uniteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libelle").value("Kilo"));
    }

    @Test
    void should_return_204_when_deleted() throws Exception {
        mockMvc.perform(delete(UniteMesureController.BASE_PATH + "/" + uniteId))
                .andExpect(status().isNoContent());

        verify(uniteMesureService).delete(uniteId);
    }
}
