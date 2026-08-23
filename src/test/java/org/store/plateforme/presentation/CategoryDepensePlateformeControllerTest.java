package org.store.plateforme.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.plateforme.application.dto.CategoryDepensePlateformeRequest;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.application.service.ICategoryDepensePlateformeService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryDepensePlateformeControllerTest {

    private MockMvc mockMvc;
    private ICategoryDepensePlateformeService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = mock(ICategoryDepensePlateformeService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new CategoryDepensePlateformeController(service))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .build();
    }

    @Test
    void should_return_201_when_category_created() throws Exception {
        CategoryDepensePlateformeRequest body = new CategoryDepensePlateformeRequest("Hébergement", "desc", true);
        when(service.create(any(CategoryDepensePlateformeRequest.class)))
                .thenReturn(new CategoryDepensePlateformeResponse(java.util.UUID.randomUUID(), "Hébergement", "desc", true));

        mockMvc.perform(post(CategoryDepensePlateformeController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Hébergement"));
    }

    @Test
    void should_return_400_when_nom_blank() throws Exception {
        CategoryDepensePlateformeRequest body = new CategoryDepensePlateformeRequest("", null, true);

        mockMvc.perform(post(CategoryDepensePlateformeController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
