package org.store.depense.presentation;

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
import org.store.depense.application.dto.CategoryDepenseRequest;
import org.store.depense.application.dto.CategoryDepenseResponse;
import org.store.depense.application.service.ICategoryDepenseService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryDepenseControllerTest {

    private MockMvc mockMvc;
    private ICategoryDepenseService categoryDepenseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        categoryDepenseService = mock(ICategoryDepenseService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new CategoryDepenseController(categoryDepenseService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void should_return_201_when_category_created() throws Exception {
        CategoryDepenseRequest body = new CategoryDepenseRequest("Loyer", "Loyer mensuel", null);
        CategoryDepenseResponse response = new CategoryDepenseResponse(UUID.randomUUID(), "Loyer", "Loyer mensuel", true);
        when(categoryDepenseService.create(any(CategoryDepenseRequest.class))).thenReturn(response);

        mockMvc.perform(post(CategoryDepenseController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("Loyer"));
    }

    @Test
    void should_return_400_when_nom_blank() throws Exception {
        CategoryDepenseRequest body = new CategoryDepenseRequest("", "desc", null);

        mockMvc.perform(post(CategoryDepenseController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_listing() throws Exception {
        CategoryDepenseResponse cat = new CategoryDepenseResponse(UUID.randomUUID(), "Loyer", null, true);
        Page<CategoryDepenseResponse> page = new PageImpl<>(List.of(cat), PageRequest.of(0, 10), 1);
        when(categoryDepenseService.findAllByCurrentEntreprise(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(CategoryDepenseController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nom").value("Loyer"));
    }
}
