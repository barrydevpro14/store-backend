package org.store.produit.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IExcelProductRowService;
import org.store.produit.application.dto.ProductImportError;
import org.store.produit.application.dto.ProductImportItem;
import org.store.produit.application.dto.ProductImportRequest;
import org.store.produit.application.dto.ProductImportResult;
import org.store.produit.application.dto.ProductRequest;
import org.store.produit.application.service.impl.ProductImportServiceImpl;
import org.store.produit.domain.model.CategoryProduct;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImportServiceImplTest {

    @Mock private IProductService productService;
    @Mock private ICategoryProductService categoryProductService;
    @Mock private IUniteMesureService uniteMesureService;
    @Mock private IExcelProductRowService excelProductRowService;
    @Mock private IMessageSourceService messageSourceService;

    @InjectMocks
    private ProductImportServiceImpl service;

    private UUID categoryId;
    private UUID pieceUnitId;

    @BeforeEach
    void setUp() {
        categoryId  = UUID.randomUUID();
        pieceUnitId = UUID.randomUUID();

        CategoryProduct stubCategory = new CategoryProduct();
        stubCategory.setId(categoryId);

        org.mockito.Mockito.lenient()
                .when(categoryProductService.existsByLibelle(any())).thenReturn(true);
        org.mockito.Mockito.lenient()
                .when(categoryProductService.findOrCreateByLibelle(any())).thenReturn(stubCategory);
        org.mockito.Mockito.lenient()
                .when(uniteMesureService.resolveIdOrPiece(any(), any())).thenReturn(pieceUnitId);
    }

    private ProductImportItem item(String uniteMesure) {
        return new ProductImportItem("REF-01", "Produit A", "desc", "Electronique", uniteMesure);
    }

    @Test
    void should_import_item_with_valid_unite_mesure() {
        UUID kgUnitId = UUID.randomUUID();
        when(productService.existsByReferenceAndNom(any(), any())).thenReturn(false);
        when(uniteMesureService.resolveIdOrPiece(eq("KG"), any())).thenReturn(kgUnitId);

        ProductImportResult result = service.importProducts(new ProductImportRequest(List.of(item("KG"))));

        assertThat(result.produitsImportes()).isEqualTo(1);
        assertThat(result.erreurs()).isEmpty();

        ArgumentCaptor<ProductRequest> captor = ArgumentCaptor.forClass(ProductRequest.class);
        verify(productService).create(captor.capture());
        assertThat(captor.getValue().uniteMesureId()).isEqualTo(kgUnitId);
    }

    @Test
    void should_fallback_to_piece_when_unite_mesure_blank() {
        when(productService.existsByReferenceAndNom(any(), any())).thenReturn(false);

        ProductImportResult result = service.importProducts(new ProductImportRequest(List.of(item(null))));

        assertThat(result.produitsImportes()).isEqualTo(1);

        ArgumentCaptor<ProductRequest> captor = ArgumentCaptor.forClass(ProductRequest.class);
        verify(productService).create(captor.capture());
        assertThat(captor.getValue().uniteMesureId()).isEqualTo(pieceUnitId);
    }

    @Test
    void should_skip_item_when_already_exists() {
        when(productService.existsByReferenceAndNom(any(), any())).thenReturn(true);

        ProductImportResult result = service.importProducts(new ProductImportRequest(List.of(item(null))));

        assertThat(result.produitsImportes()).isZero();
        assertThat(result.produitsIgnores()).isEqualTo(1);
        assertThat(result.erreurs()).isEmpty();
        verify(productService, never()).create(any());
    }

    @Test
    void should_report_error_when_categorie_blank() {
        ProductImportItem itemNoCategory = new ProductImportItem("REF-01", "Produit A", "desc", "", null);
        when(productService.existsByReferenceAndNom(any(), any())).thenReturn(false);
        when(messageSourceService.getMessage("product.import.categorie.required")).thenReturn("categorie required");

        ProductImportResult result = service.importProducts(new ProductImportRequest(List.of(itemNoCategory)));

        assertThat(result.produitsImportes()).isZero();
        assertThat(result.erreurs()).hasSize(1);
        assertThat(result.erreurs().get(0).message()).isEqualTo("categorie required");
        verify(productService, never()).create(any());
    }

    @Test
    void should_increment_categories_created_when_category_is_new() {
        when(productService.existsByReferenceAndNom(any(), any())).thenReturn(false);
        when(categoryProductService.existsByLibelle(any())).thenReturn(false);

        ProductImportResult result = service.importProducts(new ProductImportRequest(List.of(item(null))));

        assertThat(result.categoriesCreees()).isEqualTo(1);
        assertThat(result.produitsImportes()).isEqualTo(1);
    }

    @Test
    void should_report_error_when_product_create_throws() {
        when(productService.existsByReferenceAndNom(any(), any())).thenReturn(false);
        when(productService.create(any())).thenThrow(new RuntimeException("create failed"));

        ProductImportResult result = service.importProducts(new ProductImportRequest(List.of(item(null))));

        assertThat(result.produitsImportes()).isZero();
        assertThat(result.erreurs()).hasSize(1);

        ProductImportError error = result.erreurs().get(0);
        assertThat(error.reference()).isEqualTo("REF-01");
        assertThat(error.message()).isEqualTo("create failed");
    }
}
