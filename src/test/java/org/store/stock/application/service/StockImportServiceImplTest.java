package org.store.stock.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.store.common.dto.ExcelEntreeStockRow;
import org.store.common.dto.ExcelStockParseResult;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IExcelEntreeStockRowService;
import org.store.produit.application.dto.CategoryProductResponse;
import org.store.produit.application.dto.ProductRequest;
import org.store.produit.application.dto.ProductResponse;
import org.store.produit.application.dto.QualityRequest;
import org.store.produit.application.dto.QualityResponse;
import org.store.produit.application.service.ICategoryProductService;
import org.store.produit.application.service.IProductService;
import org.store.produit.application.service.IQualityService;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.Quality;
import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.application.dto.StockImportResult;
import org.store.stock.application.service.impl.StockImportServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockImportServiceImplTest {

    @Mock private IExcelEntreeStockRowService excelParser;
    @Mock private IEntreeStockService entreeStockService;
    @Mock private ICategoryProductService categoryProductService;
    @Mock private IProductService productService;
    @Mock private IQualityService qualityService;
    @Mock private IMessageSourceService messageSourceService;

    @InjectMocks
    private StockImportServiceImpl service;

    private UUID magasinId;
    private UUID fournisseurId;
    private UUID productId;
    private UUID qualityId;
    private MockMultipartFile dummyFile;

    @BeforeEach
    void setUp() {
        magasinId     = UUID.randomUUID();
        fournisseurId = UUID.randomUUID();
        productId     = UUID.randomUUID();
        qualityId     = UUID.randomUUID();
        dummyFile     = new MockMultipartFile("file", "stock.xlsx", "application/vnd.ms-excel", new byte[0]);
    }

    private ExcelEntreeStockRow validRow(int lineNumber) {
        return new ExcelEntreeStockRow(lineNumber, "REF-01", "Produit A", "Electronique", "Neuf", "10", "5.00", "8.00", null, null);
    }

    private Product stubProduct() {
        Product p = new Product();
        p.setId(productId);
        return p;
    }

    private Quality stubQuality() {
        Quality q = new Quality();
        q.setId(qualityId);
        return q;
    }

    @Test
    void should_import_all_valid_rows_when_no_errors() {
        when(excelParser.parseRows(any())).thenReturn(new ExcelStockParseResult(List.of(validRow(2), validRow(3)), List.of()));
        when(productService.findByReferenceAndNom(any(), any())).thenReturn(Optional.of(stubProduct()));
        when(qualityService.findByLibelle(any())).thenReturn(Optional.of(stubQuality()));
        when(entreeStockService.create(any())).thenReturn(List.of());

        StockImportResult result = service.importFromFile(dummyFile, magasinId, fournisseurId);

        assertThat(result.lignesImportees()).isEqualTo(2);
        assertThat(result.lignesIgnorees()).isZero();
        assertThat(result.erreurs()).isEmpty();
        verify(entreeStockService).create(any(EntreeStockRequest.class));
    }

    @Test
    void should_not_call_create_when_no_valid_rows() {
        when(excelParser.parseRows(any())).thenReturn(new ExcelStockParseResult(List.of(), List.of()));

        StockImportResult result = service.importFromFile(dummyFile, magasinId, fournisseurId);

        assertThat(result.lignesImportees()).isZero();
        verify(entreeStockService, never()).create(any());
    }

    @Test
    void should_auto_create_product_when_not_found() {
        UUID newProductId = UUID.randomUUID();
        UUID categoryId   = UUID.randomUUID();

        CategoryProduct stubCategory = new CategoryProduct();
        stubCategory.setId(categoryId);

        ProductResponse createdProduct = new ProductResponse(newProductId, "Produit A", "REF-01", null, null, null, null);

        when(excelParser.parseRows(any())).thenReturn(new ExcelStockParseResult(List.of(validRow(2)), List.of()));
        when(productService.findByReferenceAndNom(any(), any())).thenReturn(Optional.empty());
        when(categoryProductService.findOrCreateByLibelle(eq("Electronique"))).thenReturn(stubCategory);
        when(productService.create(any(ProductRequest.class))).thenReturn(createdProduct);
        when(qualityService.findByLibelle(any())).thenReturn(Optional.of(stubQuality()));
        when(entreeStockService.create(any())).thenReturn(List.of());

        StockImportResult result = service.importFromFile(dummyFile, magasinId, fournisseurId);

        assertThat(result.lignesImportees()).isEqualTo(1);
        assertThat(result.lignesIgnorees()).isZero();
        verify(categoryProductService).findOrCreateByLibelle("Electronique");
        verify(productService).create(any(ProductRequest.class));

        ArgumentCaptor<EntreeStockRequest> captor = ArgumentCaptor.forClass(EntreeStockRequest.class);
        verify(entreeStockService).create(captor.capture());
        assertThat(captor.getValue().lignes().get(0).productId()).isEqualTo(newProductId);
    }

    @Test
    void should_auto_create_quality_when_not_found() {
        UUID newQualityId = UUID.randomUUID();
        QualityResponse created = new QualityResponse(newQualityId, "Neuf", null, UUID.randomUUID());

        when(excelParser.parseRows(any())).thenReturn(new ExcelStockParseResult(List.of(validRow(2)), List.of()));
        when(productService.findByReferenceAndNom(any(), any())).thenReturn(Optional.of(stubProduct()));
        when(qualityService.findByLibelle(any())).thenReturn(Optional.empty());
        when(qualityService.create(any(QualityRequest.class))).thenReturn(created);
        when(entreeStockService.create(any())).thenReturn(List.of());

        StockImportResult result = service.importFromFile(dummyFile, magasinId, fournisseurId);

        assertThat(result.lignesImportees()).isEqualTo(1);
        assertThat(result.lignesIgnorees()).isZero();
        verify(qualityService).create(any(QualityRequest.class));

        ArgumentCaptor<EntreeStockRequest> captor = ArgumentCaptor.forClass(EntreeStockRequest.class);
        verify(entreeStockService).create(captor.capture());
        assertThat(captor.getValue().lignes().get(0).qualityId()).isEqualTo(newQualityId);
    }

    @Test
    void should_report_error_when_margin_invalid() {
        ExcelEntreeStockRow row = new ExcelEntreeStockRow(2, "REF-01", "Produit A", "Electronique", "Neuf", "10", "10.00", "8.00", null, null);
        when(excelParser.parseRows(any())).thenReturn(new ExcelStockParseResult(List.of(row), List.of()));
        when(productService.findByReferenceAndNom(any(), any())).thenReturn(Optional.of(stubProduct()));
        when(qualityService.findByLibelle(any())).thenReturn(Optional.of(stubQuality()));
        when(messageSourceService.getMessage(eq("excel.stock.import.margin.invalid"), any(Object[].class))).thenReturn("margin invalid");

        StockImportResult result = service.importFromFile(dummyFile, magasinId, fournisseurId);

        assertThat(result.lignesImportees()).isZero();
        assertThat(result.erreurs().get(0).message()).isEqualTo("margin invalid");
    }

    @Test
    void should_handle_partial_success() {
        ExcelEntreeStockRow goodRow = validRow(2);
        ExcelEntreeStockRow badRow  = new ExcelEntreeStockRow(3, "REF-01", "Produit A", "Electronique", "Neuf", "10", "10.00", "8.00", null, null);

        when(excelParser.parseRows(any())).thenReturn(new ExcelStockParseResult(List.of(goodRow, badRow), List.of()));
        when(productService.findByReferenceAndNom(any(), any())).thenReturn(Optional.of(stubProduct()));
        when(qualityService.findByLibelle(any())).thenReturn(Optional.of(stubQuality()));
        when(messageSourceService.getMessage(eq("excel.stock.import.margin.invalid"), any(Object[].class))).thenReturn("margin invalid");
        when(entreeStockService.create(any())).thenReturn(List.of());

        StockImportResult result = service.importFromFile(dummyFile, magasinId, fournisseurId);

        assertThat(result.lignesImportees()).isEqualTo(1);
        assertThat(result.lignesIgnorees()).isEqualTo(1);

        ArgumentCaptor<EntreeStockRequest> captor = ArgumentCaptor.forClass(EntreeStockRequest.class);
        verify(entreeStockService).create(captor.capture());
        assertThat(captor.getValue().lignes()).hasSize(1);
    }

    @Test
    void should_pass_through_parse_errors() {
        when(excelParser.parseRows(any())).thenReturn(
                new ExcelStockParseResult(List.of(), List.of("parse error row 2")));

        StockImportResult result = service.importFromFile(dummyFile, magasinId, fournisseurId);

        assertThat(result.lignesIgnorees()).isEqualTo(1);
        assertThat(result.erreurs().get(0).message()).isEqualTo("parse error row 2");
        verify(entreeStockService, never()).create(any());
    }

    @Test
    void should_use_correct_magasin_and_fournisseur_in_request() {
        when(excelParser.parseRows(any())).thenReturn(new ExcelStockParseResult(List.of(validRow(2)), List.of()));
        when(productService.findByReferenceAndNom(any(), any())).thenReturn(Optional.of(stubProduct()));
        when(qualityService.findByLibelle(any())).thenReturn(Optional.of(stubQuality()));
        when(entreeStockService.create(any())).thenReturn(List.of());

        service.importFromFile(dummyFile, magasinId, fournisseurId);

        ArgumentCaptor<EntreeStockRequest> captor = ArgumentCaptor.forClass(EntreeStockRequest.class);
        verify(entreeStockService).create(captor.capture());
        assertThat(captor.getValue().magasinId()).isEqualTo(magasinId);
        assertThat(captor.getValue().fournisseurId()).isEqualTo(fournisseurId);
    }

    @Test
    void should_parse_optional_date_when_present() {
        ExcelEntreeStockRow rowWithDate = new ExcelEntreeStockRow(
                2, "REF-01", "Produit A", "Electronique", "Neuf", "10", "5.00", "8.00", "LOT-1", "2027-12-31");
        when(excelParser.parseRows(any())).thenReturn(new ExcelStockParseResult(List.of(rowWithDate), List.of()));
        when(productService.findByReferenceAndNom(any(), any())).thenReturn(Optional.of(stubProduct()));
        when(qualityService.findByLibelle(any())).thenReturn(Optional.of(stubQuality()));
        when(entreeStockService.create(any())).thenReturn(List.of());

        StockImportResult result = service.importFromFile(dummyFile, magasinId, fournisseurId);

        assertThat(result.lignesImportees()).isEqualTo(1);

        ArgumentCaptor<EntreeStockRequest> captor = ArgumentCaptor.forClass(EntreeStockRequest.class);
        verify(entreeStockService).create(captor.capture());
        assertThat(captor.getValue().lignes().get(0).dateExpiration()).isNotNull();
        assertThat(captor.getValue().lignes().get(0).numeroLot()).isEqualTo("LOT-1");
    }
}
