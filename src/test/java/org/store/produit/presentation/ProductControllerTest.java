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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.produit.application.dto.CategoryProductResponse;
import org.store.produit.application.dto.ProductRequest;
import org.store.produit.application.dto.ProductResponse;
import org.store.produit.application.dto.QualityResponse;
import org.store.produit.application.service.IProductService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private MockMvc mockMvc;
    private IProductService productService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID productId;
    private UUID categoryId;
    private UUID qualityId;
    private UUID entrepriseId;

    @BeforeEach
    void setUp() {
        productService = mock(IProductService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        qualityId = UUID.randomUUID();
        entrepriseId = UUID.randomUUID();
    }

    private ProductResponse sample() {
        return new ProductResponse(productId, "Pneu 195/65 R15", "PN-195-65-R15", "Pneu été",
                new CategoryProductResponse(categoryId, "Pneus", null, entrepriseId),
                new QualityResponse(qualityId, "Premium", null, entrepriseId),
                entrepriseId, null);
    }

    private ProductRequest validBody() {
        return new ProductRequest("Pneu 195/65 R15", "PN-195-65-R15", "Pneu été", categoryId, qualityId);
    }

    @Test
    void should_return_201_when_created() throws Exception {
        when(productService.create(any(ProductRequest.class))).thenReturn(sample());

        mockMvc.perform(post(ProductController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.reference").value("PN-195-65-R15"))
                .andExpect(jsonPath("$.category.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.quality.id").value(qualityId.toString()))
                .andExpect(jsonPath("$.entrepriseId").value(entrepriseId.toString()));
    }

    @Test
    void should_return_400_when_nom_blank() throws Exception {
        ProductRequest body = new ProductRequest("", "PN-OK", null, categoryId, qualityId);

        mockMvc.perform(post(ProductController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_category_id_null() throws Exception {
        ProductRequest body = new ProductRequest("nom", "PN-OK", null, null, qualityId);

        mockMvc.perform(post(ProductController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_page_when_list() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(productService.findAllByCurrentEntreprise(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(ProductController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(productId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(productService.findResponseById(eq(productId))).thenReturn(sample());

        mockMvc.perform(get(ProductController.BASE_PATH + "/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.nom").value("Pneu 195/65 R15"));
    }

    @Test
    void should_return_200_when_updated() throws Exception {
        ProductResponse updated = new ProductResponse(productId, "Nouveau", "PN-NEW", "desc",
                new CategoryProductResponse(categoryId, "Pneus", null, entrepriseId),
                new QualityResponse(qualityId, "Premium", null, entrepriseId),
                entrepriseId, null);
        when(productService.update(eq(productId), any(ProductRequest.class))).thenReturn(updated);

        mockMvc.perform(put(ProductController.BASE_PATH + "/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Nouveau"))
                .andExpect(jsonPath("$.reference").value("PN-NEW"));
    }

    @Test
    void should_return_204_when_deleted() throws Exception {
        mockMvc.perform(delete(ProductController.BASE_PATH + "/" + productId))
                .andExpect(status().isNoContent());

        verify(productService).delete(productId);
    }

    @Test
    void should_return_200_when_image_uploaded() throws Exception {
        UUID imagePrincipalId = UUID.randomUUID();
        ProductResponse withImage = new ProductResponse(productId, "Pneu", "PN-1", "desc",
                new CategoryProductResponse(categoryId, "Pneus", null, entrepriseId),
                new QualityResponse(qualityId, "Premium", null, entrepriseId),
                entrepriseId, imagePrincipalId);
        when(productService.uploadImagePrincipal(eq(productId), any())).thenReturn(withImage);

        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart(ProductController.BASE_PATH + "/" + productId + "/image")
                        .file(file)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagePrincipalId").value(imagePrincipalId.toString()));
    }

    @Test
    void should_return_204_when_image_deleted() throws Exception {
        mockMvc.perform(delete(ProductController.BASE_PATH + "/" + productId + "/image"))
                .andExpect(status().isNoContent());

        verify(productService).deleteImagePrincipal(productId);
    }

    @Test
    void should_return_201_when_multiple_images_uploaded() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        when(productService.uploadImages(eq(productId), any())).thenReturn(List.of(id1, id2));

        MockMultipartFile f1 = new MockMultipartFile("files", "a.png", "image/png", new byte[]{1});
        MockMultipartFile f2 = new MockMultipartFile("files", "b.jpg", "image/jpeg", new byte[]{2});

        mockMvc.perform(multipart(ProductController.BASE_PATH + "/" + productId + "/images")
                        .file(f1)
                        .file(f2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0]").value(id1.toString()))
                .andExpect(jsonPath("$[1]").value(id2.toString()));
    }
}
