package org.store.stock.application.service;

import org.springframework.web.multipart.MultipartFile;
import org.store.stock.application.dto.StockImportResult;

import java.util.UUID;

public interface IStockImportService {

    /** Importe des entrées stock depuis un fichier Excel. Les lignes valides sont committées en un seul batch. */
    StockImportResult importFromFile(MultipartFile file, UUID magasinId, UUID fournisseurId);
}
