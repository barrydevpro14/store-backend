package org.store.stock.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.stock.application.dto.AjustementStockRequest;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.stock.application.service.IAjustementStockService;

@RestController
@RequestMapping(AjustementStockController.BASE_PATH)
public class AjustementStockController {

    public static final String BASE_PATH = "/api/v1/stocks/adjustments";

    private final IAjustementStockService ajustementStockService;

    public AjustementStockController(IAjustementStockService ajustementStockService) {
        this.ajustementStockService = ajustementStockService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STOCK_ADJUSTMENT')")
    public ResponseEntity<MouvementStockResponse> create(@Valid @RequestBody AjustementStockRequest ajustementStockRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ajustementStockService.create(ajustementStockRequest));
    }
}
