package org.store.reporting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.stock.application.dto.StockValuationResponse;
import org.store.stock.application.service.IStockService;

import java.util.UUID;

@RestController
public class StockValuationController {

    private final IStockService stockService;

    public StockValuationController(IStockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/api/v1/stocks/valuation")
    @PreAuthorize("hasAuthority('STOCK_READ')")
    public ResponseEntity<StockValuationResponse> computeValuation(@RequestParam UUID magasinId) {
        return ResponseEntity.ok(stockService.computeValuation(magasinId));
    }
}
