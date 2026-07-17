package org.store.common.service.impl;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ExcelEntreeStockRow;
import org.store.common.dto.ExcelStockParseResult;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IExcelEntreeStockRowService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Parse un fichier Excel dont les colonnes suivent l'ordre :
 * referenceProduit | nomProduit | categorie | qualite | quantite | prixAchat | prixVente | numeroLot | dateExpiration.
 * Les 7 premières colonnes sont obligatoires ; les 2 dernières sont optionnelles.
 * Les lignes invalides sont collectées dans {@link ExcelStockParseResult#errors()} sans interrompre le traitement.
 */
@Service
public class ExcelEntreeStockRowServiceImpl implements IExcelEntreeStockRowService {

    private record StockRowContext(DataFormatter formatter, List<ExcelEntreeStockRow> rows, List<String> errors) {}

    private final IMessageSourceService messageSourceService;

    public ExcelEntreeStockRowServiceImpl(IMessageSourceService messageSourceService) {
        this.messageSourceService = messageSourceService;
    }

    @Override
    public ExcelStockParseResult parseRows(MultipartFile multipartFile) {
        try (Workbook workbook = WorkbookFactory.create(multipartFile.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            List<ExcelEntreeStockRow> rows = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            StockRowContext context = new StockRowContext(formatter, rows, errors);

            IntStream.rangeClosed(1, sheet.getLastRowNum())
                    .forEach(rowIndex -> processRow(context, sheet.getRow(rowIndex), rowIndex + 1));

            return new ExcelStockParseResult(rows, errors);

        } catch (IOException e) {
            throw new BadArgumentException("excel.import.fileReadError");
        }
    }

    public void processRow(StockRowContext context, Row row, int lineNumber) {
        if (row == null) return;

        DataFormatter formatter = context.formatter();

        String referenceProduit  = extractCellValue(formatter, row, 0);
        String nomProduit        = extractCellValue(formatter, row, 1);
        String categorie         = extractCellValue(formatter, row, 2);
        String qualite           = extractCellValue(formatter, row, 3);
        String quantiteRaw       = extractCellValue(formatter, row, 4);
        String prixAchatRaw      = extractCellValue(formatter, row, 5);
        String prixVenteRaw      = extractCellValue(formatter, row, 6);
        String numeroLot         = extractCellValue(formatter, row, 7);
        String dateExpirationRaw = extractCellValue(formatter, row, 8);

        if (referenceProduit.isBlank()) {
            context.errors().add(msg("excel.stock.import.row.referenceProduit.required", lineNumber));
            return;
        }
        if (nomProduit.isBlank()) {
            context.errors().add(msg("excel.stock.import.row.nomProduit.required", lineNumber));
            return;
        }
        if (categorie.isBlank()) {
            context.errors().add(msg("excel.stock.import.row.categorie.required", lineNumber));
            return;
        }
        if (qualite.isBlank()) {
            context.errors().add(msg("excel.stock.import.row.qualite.required", lineNumber));
            return;
        }
        if (quantiteRaw.isBlank()) {
            context.errors().add(msg("excel.stock.import.row.quantite.required", lineNumber));
            return;
        }
        if (!isPositiveInteger(quantiteRaw)) {
            context.errors().add(msg("excel.stock.import.row.quantite.invalid", lineNumber));
            return;
        }
        if (prixAchatRaw.isBlank()) {
            context.errors().add(msg("excel.stock.import.row.prixAchat.required", lineNumber));
            return;
        }
        if (!isPositiveDecimal(prixAchatRaw)) {
            context.errors().add(msg("excel.stock.import.row.prixAchat.invalid", lineNumber));
            return;
        }
        if (prixVenteRaw.isBlank()) {
            context.errors().add(msg("excel.stock.import.row.prixVente.required", lineNumber));
            return;
        }
        if (!isPositiveDecimal(prixVenteRaw)) {
            context.errors().add(msg("excel.stock.import.row.prixVente.invalid", lineNumber));
            return;
        }
        if (!dateExpirationRaw.isBlank() && !isValidDate(dateExpirationRaw)) {
            context.errors().add(msg("excel.stock.import.row.dateExpiration.invalid", lineNumber));
            return;
        }

        context.rows().add(new ExcelEntreeStockRow(
                lineNumber,
                referenceProduit,
                nomProduit,
                categorie,
                qualite,
                quantiteRaw,
                normalizeDecimal(prixAchatRaw),
                normalizeDecimal(prixVenteRaw),
                numeroLot.isBlank() ? null : numeroLot,
                dateExpirationRaw.isBlank() ? null : dateExpirationRaw
        ));
    }

    public String extractCellValue(DataFormatter formatter, Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private boolean isPositiveInteger(String value) {
        try {
            return Integer.parseInt(value.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isPositiveDecimal(String value) {
        try {
            return new java.math.BigDecimal(normalizeDecimal(value.trim())).compareTo(java.math.BigDecimal.ZERO) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidDate(String value) {
        try {
            LocalDate.parse(value.trim());
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /** Normalise le séparateur décimal : remplace la virgule par un point (format FR → standard). */
    private static String normalizeDecimal(String value) {
        return value.replace(",", ".");
    }

    private String msg(String key, int lineNumber) {
        return messageSourceService.getMessage(key, new Object[]{lineNumber});
    }
}
