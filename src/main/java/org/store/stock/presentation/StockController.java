package org.store.stock.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.stock.application.dto.BelowThresholdFilter;
import org.store.stock.application.dto.ExpiringLotResponse;
import org.store.stock.application.dto.ExpiringLotsFilter;
import org.store.common.dto.DataCountResponse;
import org.store.stock.application.dto.StockFilter;
import org.store.stock.application.dto.StockResponse;
import org.store.stock.application.dto.StockThresholdRequest;
import org.store.stock.application.service.IExpiringLotsService;
import org.store.stock.application.service.IStockService;

import java.util.UUID;

@RestController
@RequestMapping(StockController.BASE_PATH)
@Validated
public class StockController {

    public static final String BASE_PATH = "/api/v1/stocks";

    private final IStockService stockService;
    private final IExpiringLotsService expiringLotsService;

    public StockController(IStockService stockService, IExpiringLotsService expiringLotsService) {
        this.stockService = stockService;
        this.expiringLotsService = expiringLotsService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_READ')")
    public ResponseEntity<Page<StockResponse>> list(@RequestParam UUID magasinId,
                                                    @RequestParam(required = false) String productName,
                                                    @RequestParam(required = false) String startDate,
                                                    @RequestParam(required = false) String endDate,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(stockService.findAllByCurrentEntreprise(
                new StockFilter(magasinId, productName, startDate, endDate, page, size)
        ));
    }

    @GetMapping("/below-threshold")
    @PreAuthorize("hasAuthority('STOCK_READ')")
    public ResponseEntity<Page<StockResponse>> listBelowThreshold(@RequestParam UUID magasinId,
                                                                  @RequestParam(required = false) UUID productId,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(stockService.findBelowThresholdByCurrentEntreprise(
                new BelowThresholdFilter(magasinId, productId, page, size)
        ));
    }

    @GetMapping("/below-threshold/count")
    @PreAuthorize("hasAuthority('STOCK_READ')")
    public ResponseEntity<DataCountResponse> countBelowThreshold(@RequestParam UUID magasinId) {
        return ResponseEntity.ok(new DataCountResponse(stockService.countBelowThresholdByCurrentEntreprise(magasinId)));
    }

    @GetMapping("/expiring-lots")
    @PreAuthorize("hasAuthority('STOCK_READ')")
    public ResponseEntity<Page<ExpiringLotResponse>> listExpiringLots(@RequestParam UUID magasinId,
                                                                      @RequestParam(required = false) UUID productId,
                                                                      @RequestParam(defaultValue = "30") int daysAhead,
                                                                      @RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(expiringLotsService.findExpiringLots(
                new ExpiringLotsFilter(magasinId, productId, daysAhead, page, size)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STOCK_READ')")
    public ResponseEntity<StockResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(stockService.findResponseById(id));
    }

    @PatchMapping("/{id}/threshold")
    @PreAuthorize("hasAuthority('STOCK_ADJUSTMENT')")
    public ResponseEntity<StockResponse> updateThreshold(@PathVariable UUID id,
                                                         @Valid @RequestBody StockThresholdRequest stockThresholdRequest) {
        return ResponseEntity.ok(stockService.updateThreshold(id, stockThresholdRequest));
    }
}
