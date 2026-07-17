package org.store.common.service.impl;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.store.common.dto.ExcelEntreeStockRow;
import org.store.common.dto.ExcelStockParseResult;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.i18n.IMessageSourceService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ExcelEntreeStockRowServiceImplTest {

    @Mock private IMessageSourceService messageSourceService;

    private ExcelEntreeStockRowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExcelEntreeStockRowServiceImpl(messageSourceService);
        lenient().when(messageSourceService.getMessage(anyString(), any(Object[].class))).thenAnswer(inv -> inv.getArgument(0));
    }

    /** Colonnes : referenceProduit(0) nomProduit(1) categorie(2) qualite(3) quantite(4) prixAchat(5) prixVente(6) numeroLot(7) dateExpiration(8) */
    private MockMultipartFile buildFile(Object... colValues) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            sheet.createRow(0).createCell(0).setCellValue("header");

            Row data = sheet.createRow(1);
            for (int i = 0; i < colValues.length; i++) {
                if (colValues[i] != null) {
                    data.createCell(i).setCellValue(colValues[i].toString());
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile("file", "stock.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    private MockMultipartFile validFile() throws IOException {
        return buildFile("REF-01", "Produit A", "Electronique", "Neuf", "10", "5.00", "8.00", "LOT-1", "2027-12-31");
    }

    @Test
    void should_parse_valid_complete_row() throws IOException {
        ExcelStockParseResult result = service.parseRows(validFile());

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).hasSize(1);
        ExcelEntreeStockRow row = result.rows().get(0);
        assertThat(row.referenceProduit()).isEqualTo("REF-01");
        assertThat(row.nomProduit()).isEqualTo("Produit A");
        assertThat(row.categorie()).isEqualTo("Electronique");
        assertThat(row.qualite()).isEqualTo("Neuf");
        assertThat(row.quantite()).isEqualTo("10");
        assertThat(row.prixAchat()).isEqualTo("5.00");
        assertThat(row.prixVente()).isEqualTo("8.00");
        assertThat(row.numeroLot()).isEqualTo("LOT-1");
        assertThat(row.dateExpiration()).isEqualTo("2027-12-31");
        assertThat(row.lineNumber()).isEqualTo(2);
    }

    @Test
    void should_parse_row_with_optional_fields_absent() throws IOException {
        MockMultipartFile file = buildFile("REF-01", "Produit A", "Electronique", "Neuf", "10", "5.00", "8.00");

        ExcelStockParseResult result = service.parseRows(file);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0).numeroLot()).isNull();
        assertThat(result.rows().get(0).dateExpiration()).isNull();
    }

    @Test
    void should_normalize_comma_decimal_separator() throws IOException {
        MockMultipartFile file = buildFile("REF-01", "Produit A", "Electronique", "Neuf", "10", "5,50", "9,00");

        ExcelStockParseResult result = service.parseRows(file);

        assertThat(result.errors()).isEmpty();
        assertThat(result.rows().get(0).prixAchat()).isEqualTo("5.50");
        assertThat(result.rows().get(0).prixVente()).isEqualTo("9.00");
    }

    @Test
    void should_report_error_when_referenceProduit_blank() throws IOException {
        MockMultipartFile file = buildFile("", "Produit A", "Electronique", "Neuf", "10", "5.00", "8.00");

        ExcelStockParseResult result = service.parseRows(file);

        assertThat(result.rows()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0)).contains("excel.stock.import.row.referenceProduit.required");
    }

    @Test
    void should_report_error_when_categorie_blank() throws IOException {
        MockMultipartFile file = buildFile("REF-01", "Produit A", "", "Neuf", "10", "5.00", "8.00");

        ExcelStockParseResult result = service.parseRows(file);

        assertThat(result.rows()).isEmpty();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0)).contains("excel.stock.import.row.categorie.required");
    }

    @Test
    void should_report_error_when_quantite_not_integer() throws IOException {
        MockMultipartFile file = buildFile("REF-01", "Produit A", "Electronique", "Neuf", "abc", "5.00", "8.00");

        ExcelStockParseResult result = service.parseRows(file);

        assertThat(result.rows()).isEmpty();
        assertThat(result.errors().get(0)).contains("excel.stock.import.row.quantite.invalid");
    }

    @Test
    void should_report_error_when_quantite_zero() throws IOException {
        MockMultipartFile file = buildFile("REF-01", "Produit A", "Electronique", "Neuf", "0", "5.00", "8.00");

        ExcelStockParseResult result = service.parseRows(file);

        assertThat(result.rows()).isEmpty();
        assertThat(result.errors().get(0)).contains("excel.stock.import.row.quantite.invalid");
    }

    @Test
    void should_report_error_when_prixAchat_not_numeric() throws IOException {
        MockMultipartFile file = buildFile("REF-01", "Produit A", "Electronique", "Neuf", "5", "abc", "8.00");

        ExcelStockParseResult result = service.parseRows(file);

        assertThat(result.rows()).isEmpty();
        assertThat(result.errors().get(0)).contains("excel.stock.import.row.prixAchat.invalid");
    }

    @Test
    void should_report_error_when_date_expiration_invalid_format() throws IOException {
        MockMultipartFile file = buildFile("REF-01", "Produit A", "Electronique", "Neuf", "10", "5.00", "8.00", null, "31/12/2027");

        ExcelStockParseResult result = service.parseRows(file);

        assertThat(result.rows()).isEmpty();
        assertThat(result.errors().get(0)).contains("excel.stock.import.row.dateExpiration.invalid");
    }

    @Test
    void should_throw_BadArgumentException_on_unreadable_file() {
        MockMultipartFile corrupt = new MockMultipartFile("file", "bad.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "not-an-excel-file".getBytes());

        assertThatThrownBy(() -> service.parseRows(corrupt))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void should_collect_errors_across_multiple_rows() throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet();
            sheet.createRow(0).createCell(0).setCellValue("header");

            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("");

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("REF-01");
            row2.createCell(1).setCellValue("Produit A");
            row2.createCell(2).setCellValue("Electronique");
            row2.createCell(3).setCellValue("Neuf");
            row2.createCell(4).setCellValue("10");
            row2.createCell(5).setCellValue("5.00");
            row2.createCell(6).setCellValue("8.00");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            MockMultipartFile file = new MockMultipartFile("file", "stock.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());

            ExcelStockParseResult result = service.parseRows(file);

            assertThat(result.errors()).hasSize(1);
            assertThat(result.rows()).hasSize(1);
        }
    }
}
