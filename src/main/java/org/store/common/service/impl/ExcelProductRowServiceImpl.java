package org.store.common.service.impl;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ExcelParseResult;
import org.store.common.dto.ExcelProductRow;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IExcelProductRowService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Shared parser for Excel product files whose columns follow the order:
 * reference | libelle | description | categorie.
 * Reference, libelle, and categorie are mandatory; rows that fail validation
 * are collected as i18n-resolved error messages in the returned {@link ExcelParseResult}.
 */
@Service
public class ExcelProductRowServiceImpl implements IExcelProductRowService {

    private record ExcelRowContext(DataFormatter formatter, List<ExcelProductRow> rows, List<String> errors) {}

    private final IMessageSourceService messageSourceService;

    public ExcelProductRowServiceImpl(IMessageSourceService messageSourceService) {
        this.messageSourceService = messageSourceService;
    }

    /**
     * Reads every data row from the first sheet, validates mandatory fields, and returns
     * valid rows alongside i18n error messages for any row that failed validation.
     */
    @Override
    public ExcelParseResult parseRows(MultipartFile multipartFile) {
        try (Workbook workbook = WorkbookFactory.create(multipartFile.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<ExcelProductRow> rows = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            ExcelRowContext context = new ExcelRowContext(formatter, rows, errors);

            IntStream.rangeClosed(1, sheet.getLastRowNum())
                    .forEach(rowIndex -> processRow(context, sheet.getRow(rowIndex), rowIndex + 1));

            return new ExcelParseResult(rows, errors);

        } catch (IOException ioException) {
            throw new BadArgumentException("excel.import.fileReadError");
        }
    }

    /**
     * Validates a single Excel row and appends it to rows on success or an error message on failure.
     * Null rows (fully empty spreadsheet lines) are silently skipped.
     */
    public void processRow(ExcelRowContext context, Row row, int lineNumber) {
        if (row == null) {
            return;
        }

        DataFormatter formatter = context.formatter();
        String reference   = extractCellValue(formatter, row, 0);
        String libelle     = extractCellValue(formatter, row, 1);
        String description = extractCellValue(formatter, row, 2);
        String categorie   = extractCellValue(formatter, row, 3);

        if (reference.isBlank()) {
            context.errors().add(messageSourceService.getMessage(
                    "excel.import.row.reference.required", new Object[]{lineNumber}));
            return;
        }

        if (libelle.isBlank()) {
            context.errors().add(messageSourceService.getMessage(
                    "excel.import.row.libelle.required", new Object[]{lineNumber}));
            return;
        }

        if (categorie.isBlank()) {
            context.errors().add(messageSourceService.getMessage(
                    "excel.import.row.categorie.required", new Object[]{lineNumber}));
            return;
        }

        context.rows().add(new ExcelProductRow(
                reference,
                libelle,
                description.isBlank() ? null : description,
                categorie
        ));
    }

    /**
     * Extracts the trimmed string value of the cell at the given column index, returning an empty string when absent.
     */
    public String extractCellValue(DataFormatter formatter, Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }
}
