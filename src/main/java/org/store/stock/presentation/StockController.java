package org.store.stock.presentation;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.stock.application.dto.StockFilter;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.application.service.IStockService;

import java.util.UUID;

@RestController
@RequestMapping(StockController.BASE_PATH)
@Validated
public class StockController {

    public static final String BASE_PATH = "/api/v1/stocks";

    private final IStockService stockService;

    public StockController(IStockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_READ')")
    public ResponseEntity<Page<StockResponse>> list(@RequestParam UUID magasinId,
                                                    @RequestParam(required = false) UUID productId,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(stockService.findAllByCurrentEntreprise(
                new StockFilter(magasinId, productId, page, size)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STOCK_READ')")
    public ResponseEntity<StockResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(stockService.findResponseById(id));
    }
}
