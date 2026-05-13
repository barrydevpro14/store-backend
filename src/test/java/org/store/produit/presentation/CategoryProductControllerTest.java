package org.store.produit.presentation;

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
import org.store.produit.application.dto.CategoryProductRequest;
import org.store.produit.application.dto.CategoryProductResponse;
import org.store.produit.application.service.ICategoryProductService;

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

class CategoryProductControllerTest {

    private MockMvc mockMvc;
    private ICategoryProductService categoryProductService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID categoryId;
    private UUID entrepriseId;

    @BeforeEach
    void setUp() {
        categoryProductService = mock(ICategoryProductService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new CategoryProductController(categoryProductService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        categoryId = UUID.randomUUID();
        entrepriseId = UUID.randomUUID();
    }

    private CategoryProductResponse sample() {
        return new CategoryProductResponse(categoryId, "Pneus", "Cat. pneus", entrepriseId);
    }

    @Test
    void should_return_201_when_created() throws Exception {
        CategoryProductRequest body = new CategoryProductRequest("Pneus", "Cat. pneus");
        when(categoryProductService.create(any(CategoryProductRequest.class))).thenReturn(sample());

        mockMvc.perform(post(CategoryProductController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.libelle").value("Pneus"))
                .andExpect(jsonPath("$.entrepriseId").value(entrepriseId.toString()));
    }

    @Test
    void should_return_400_when_libelle_blank() throws Exception {
        CategoryProductRequest body = new CategoryProductRequest("", "desc");

        mockMvc.perform(post(CategoryProductController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_page_when_list() throws Exception {
        Page<CategoryProductResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(categoryProductService.findAllByCurrentEntreprise(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(CategoryProductController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(categoryId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(categoryProductService.findResponseById(eq(categoryId))).thenReturn(sample());

        mockMvc.perform(get(CategoryProductController.BASE_PATH + "/" + categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.libelle").value("Pneus"));
    }

    @Test
    void should_return_200_when_updated() throws Exception {
        CategoryProductRequest body = new CategoryProductRequest("Filtres", "Cat. filtres");
        CategoryProductResponse updated = new CategoryProductResponse(categoryId, "Filtres", "Cat. filtres", entrepriseId);
        when(categoryProductService.update(eq(categoryId), any(CategoryProductRequest.class))).thenReturn(updated);

        mockMvc.perform(put(CategoryProductController.BASE_PATH + "/" + categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.libelle").value("Filtres"));
    }

    @Test
    void should_return_204_when_deleted() throws Exception {
        mockMvc.perform(delete(CategoryProductController.BASE_PATH + "/" + categoryId))
                .andExpect(status().isNoContent());

        verify(categoryProductService).delete(categoryId);
    }
}
