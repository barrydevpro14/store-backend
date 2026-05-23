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
import org.store.produit.application.dto.QualityFilter;
import org.store.produit.application.dto.QualityRequest;
import org.store.produit.application.dto.QualityResponse;
import org.store.produit.application.service.IQualityService;

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

class QualityControllerTest {

    private MockMvc mockMvc;
    private IQualityService qualityService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID qualityId;
    private UUID entrepriseId;

    @BeforeEach
    void setUp() {
        qualityService = mock(IQualityService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new QualityController(qualityService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();

        qualityId = UUID.randomUUID();
        entrepriseId = UUID.randomUUID();
    }

    private QualityResponse sample() {
        return new QualityResponse(qualityId, "Premium", "Qualité haut de gamme", entrepriseId);
    }

    @Test
    void should_return_201_when_created() throws Exception {
        QualityRequest body = new QualityRequest("Premium", "Qualité haut de gamme");
        when(qualityService.create(any(QualityRequest.class))).thenReturn(sample());

        mockMvc.perform(post(QualityController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(qualityId.toString()))
                .andExpect(jsonPath("$.libelle").value("Premium"))
                .andExpect(jsonPath("$.entrepriseId").value(entrepriseId.toString()));
    }

    @Test
    void should_return_400_when_libelle_blank() throws Exception {
        QualityRequest body = new QualityRequest("", "desc");

        mockMvc.perform(post(QualityController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_page_when_list() throws Exception {
        Page<QualityResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(qualityService.findAll(any(QualityFilter.class))).thenReturn(page);

        mockMvc.perform(get(QualityController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(qualityId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(qualityService.findResponseById(eq(qualityId))).thenReturn(sample());

        mockMvc.perform(get(QualityController.BASE_PATH + "/" + qualityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(qualityId.toString()))
                .andExpect(jsonPath("$.libelle").value("Premium"));
    }

    @Test
    void should_return_200_when_updated() throws Exception {
        QualityRequest body = new QualityRequest("Standard", "Qualité standard");
        QualityResponse updated = new QualityResponse(qualityId, "Standard", "Qualité standard", entrepriseId);
        when(qualityService.update(eq(qualityId), any(QualityRequest.class))).thenReturn(updated);

        mockMvc.perform(put(QualityController.BASE_PATH + "/" + qualityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libelle").value("Standard"));
    }

    @Test
    void should_return_204_when_deleted() throws Exception {
        mockMvc.perform(delete(QualityController.BASE_PATH + "/" + qualityId))
                .andExpect(status().isNoContent());

        verify(qualityService).delete(qualityId);
    }
}
