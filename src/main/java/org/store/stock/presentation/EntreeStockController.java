package org.store.stock.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.application.dto.EntreeStockResponse;
import org.store.stock.application.dto.StockImportResult;
import org.store.stock.application.service.IEntreeStockService;
import org.store.stock.application.service.IStockImportService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(EntreeStockController.BASE_PATH)
public class EntreeStockController {

    public static final String BASE_PATH = "/api/v1/stocks/entries";

    private final IEntreeStockService entreeStockService;
    private final IStockImportService stockImportService;

    public EntreeStockController(IEntreeStockService entreeStockService, IStockImportService stockImportService) {
        this.entreeStockService = entreeStockService;
        this.stockImportService = stockImportService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STOCK_ENTRY')")
    public ResponseEntity<List<EntreeStockResponse>> create(@Valid @RequestBody EntreeStockRequest entreeStockRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entreeStockService.create(entreeStockRequest));
    }

    @PostMapping("/file")
    @PreAuthorize("hasAuthority('STOCK_IMPORT')")
    public ResponseEntity<StockImportResult> importFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam UUID magasinId,
            @RequestParam UUID fournisseurId) {
        return ResponseEntity.ok(stockImportService.importFromFile(file, magasinId, fournisseurId));
    }
}
