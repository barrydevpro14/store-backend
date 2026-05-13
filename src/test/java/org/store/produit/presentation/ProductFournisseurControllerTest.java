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
import org.store.achat.application.dto.FournisseurSummaryResponse;
import org.store.common.exceptions.GlobalException;
import org.store.common.i18n.IMessageSourceService;
import org.store.produit.application.dto.ProductFournisseurRequest;
import org.store.produit.application.dto.ProductFournisseurResponse;
import org.store.produit.application.dto.ProductSummaryResponse;
import org.store.produit.application.service.IProductFournisseurService;

import java.math.BigDecimal;
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

class ProductFournisseurControllerTest {

    private MockMvc mockMvc;
    private IProductFournisseurService productFournisseurService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID productFournisseurId;
    private UUID productId;
    private UUID fournisseurId;

    @BeforeEach
    void setUp() {
        productFournisseurService = mock(IProductFournisseurService.class);
        IMessageSourceService messageSourceService = mock(IMessageSourceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ProductFournisseurController(productFournisseurService))
                .setControllerAdvice(new GlobalException(messageSourceService))
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();

        productFournisseurId = UUID.randomUUID();
        productId = UUID.randomUUID();
        fournisseurId = UUID.randomUUID();
    }

    private ProductFournisseurResponse sample() {
        return new ProductFournisseurResponse(
                productFournisseurId,
                new ProductSummaryResponse(productId, "Pneu 195/65 R15", "PN-195"),
                new FournisseurSummaryResponse(fournisseurId, "Pneus Maroc SARL"),
                new BigDecimal("12.50"), "REF-FRN-001", "Maroc"
        );
    }

    private ProductFournisseurRequest validBody() {
        return new ProductFournisseurRequest(productId, fournisseurId, new BigDecimal("12.50"), "REF-FRN-001", "Maroc");
    }

    @Test
    void should_return_201_when_created() throws Exception {
        when(productFournisseurService.create(any(ProductFournisseurRequest.class))).thenReturn(sample());

        mockMvc.perform(post(ProductFournisseurController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(productFournisseurId.toString()))
                .andExpect(jsonPath("$.product.id").value(productId.toString()))
                .andExpect(jsonPath("$.fournisseur.id").value(fournisseurId.toString()))
                .andExpect(jsonPath("$.prixAchat").value(12.50))
                .andExpect(jsonPath("$.referenceFournisseur").value("REF-FRN-001"))
                .andExpect(jsonPath("$.origine").value("Maroc"));
    }

    @Test
    void should_return_400_when_product_id_null() throws Exception {
        ProductFournisseurRequest body = new ProductFournisseurRequest(null, fournisseurId, new BigDecimal("12.50"), null, null);

        mockMvc.perform(post(ProductFournisseurController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_400_when_prix_achat_zero_or_negative() throws Exception {
        ProductFournisseurRequest body = new ProductFournisseurRequest(productId, fournisseurId, BigDecimal.ZERO, null, null);

        mockMvc.perform(post(ProductFournisseurController.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return_200_with_page_when_list_all() throws Exception {
        Page<ProductFournisseurResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(productFournisseurService.findAllByCurrentEntreprise(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(ProductFournisseurController.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(productFournisseurId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_return_200_with_page_when_filter_by_product() throws Exception {
        Page<ProductFournisseurResponse> page = new PageImpl<>(List.of(sample()), PageRequest.of(0, 10), 1);
        when(productFournisseurService.findAllByProductId(eq(productId), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get(ProductFournisseurController.BASE_PATH).param("productId", productId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].product.id").value(productId.toString()));

        verify(productFournisseurService).findAllByProductId(eq(productId), any(Pageable.class));
    }

    @Test
    void should_return_200_when_get_by_id() throws Exception {
        when(productFournisseurService.findResponseById(eq(productFournisseurId))).thenReturn(sample());

        mockMvc.perform(get(ProductFournisseurController.BASE_PATH + "/" + productFournisseurId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productFournisseurId.toString()));
    }

    @Test
    void should_return_200_when_updated() throws Exception {
        ProductFournisseurResponse updated = new ProductFournisseurResponse(
                productFournisseurId,
                new ProductSummaryResponse(productId, "Pneu 195/65 R15", "PN-195"),
                new FournisseurSummaryResponse(fournisseurId, "Pneus Maroc SARL"),
                new BigDecimal("20.00"), "REF-NEW", "France"
        );
        when(productFournisseurService.update(eq(productFournisseurId), any(ProductFournisseurRequest.class))).thenReturn(updated);

        ProductFournisseurRequest body = new ProductFournisseurRequest(productId, fournisseurId, new BigDecimal("20.00"), "REF-NEW", "France");

        mockMvc.perform(put(ProductFournisseurController.BASE_PATH + "/" + productFournisseurId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixAchat").value(20.00))
                .andExpect(jsonPath("$.referenceFournisseur").value("REF-NEW"))
                .andExpect(jsonPath("$.origine").value("France"));
    }

    @Test
    void should_return_204_when_deleted() throws Exception {
        mockMvc.perform(delete(ProductFournisseurController.BASE_PATH + "/" + productFournisseurId))
                .andExpect(status().isNoContent());

        verify(productFournisseurService).delete(productFournisseurId);
    }
}
