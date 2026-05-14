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
import org.store.common.dto.ImageDownloadResponse;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.produit.application.dto.CategoryProductSummaryResponse;
import org.store.produit.application.dto.ImageMetadataResponse;
import org.store.produit.application.dto.ProductRequest;
import org.store.produit.application.dto.ProductResponse;
import org.store.produit.application.dto.QualitySummaryResponse;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
                new CategoryProductSummaryResponse(categoryId, "Pneus"),
                new QualitySummaryResponse(qualityId, "Premium"),
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
                .andExpect(jsonPath("$.category.libelle").value("Pneus"))
                .andExpect(jsonPath("$.quality.id").value(qualityId.toString()))
                .andExpect(jsonPath("$.quality.libelle").value("Premium"))
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
                new CategoryProductSummaryResponse(categoryId, "Pneus"),
                new QualitySummaryResponse(qualityId, "Premium"),
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
        ProductResponse withImage = new ProductResponse(productId, "Pneu", "PN-1", "desc",
                new CategoryProductSummaryResponse(categoryId, "Pneus"),
                new QualitySummaryResponse(qualityId, "Premium"),
                entrepriseId,
                "/api/v1/products/" + productId + "/image");
        when(productService.uploadImagePrincipal(eq(productId), any())).thenReturn(withImage);

        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart(ProductController.BASE_PATH + "/" + productId + "/image")
                        .file(file)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.image").value("/api/v1/products/" + productId + "/image"));
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

    @Test
    void should_serve_image_principal_with_detected_content_type() throws Exception {
        byte[] payload = new byte[]{1, 2, 3};
        when(productService.getImagePrincipal(eq(productId)))
                .thenReturn(new ImageDownloadResponse(payload, "image/png"));

        mockMvc.perform(get(ProductController.BASE_PATH + "/" + productId + "/image"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(payload));
    }

    @Test
    void should_serve_gallery_image_with_detected_content_type() throws Exception {
        UUID imgId = UUID.randomUUID();
        byte[] payload = new byte[]{4, 5, 6};
        when(productService.getImage(eq(productId), eq(imgId)))
                .thenReturn(new ImageDownloadResponse(payload, "image/jpeg"));

        mockMvc.perform(get(ProductController.BASE_PATH + "/" + productId + "/images/" + imgId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(content().bytes(payload));
    }

    @Test
    void should_return_204_when_gallery_image_deleted() throws Exception {
        UUID imgId = UUID.randomUUID();

        mockMvc.perform(delete(ProductController.BASE_PATH + "/" + productId + "/images/" + imgId))
                .andExpect(status().isNoContent());

        verify(productService).deleteImage(productId, imgId);
    }

    @Test
    void should_return_gallery_metadata_list() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        ImageMetadataResponse m1 = new ImageMetadataResponse(id1, "2026-05-13", "image/png",
                "/api/v1/products/" + productId + "/images/" + id1);
        ImageMetadataResponse m2 = new ImageMetadataResponse(id2, "2026-05-14", "image/jpeg",
                "/api/v1/products/" + productId + "/images/" + id2);
        when(productService.listImages(eq(productId))).thenReturn(List.of(m1, m2));

        mockMvc.perform(get(ProductController.BASE_PATH + "/" + productId + "/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id1.toString()))
                .andExpect(jsonPath("$[0].contentType").value("image/png"))
                .andExpect(jsonPath("$[0].url").value("/api/v1/products/" + productId + "/images/" + id1))
                .andExpect(jsonPath("$[1].id").value(id2.toString()));
    }
}
