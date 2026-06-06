package org.store.stock.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.stock.application.dto.EntreeStockRequest;
import org.store.stock.application.dto.EntreeStockResponse;
import org.store.stock.application.service.IEntreeStockService;

@RestController
@RequestMapping(EntreeStockController.BASE_PATH)
public class EntreeStockController {

    public static final String BASE_PATH = "/api/v1/stocks/entries";

    private final IEntreeStockService entreeStockService;

    public EntreeStockController(IEntreeStockService entreeStockService) {
        this.entreeStockService = entreeStockService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STOCK_ENTRY')")
    public ResponseEntity<EntreeStockResponse> create(@Valid @RequestBody EntreeStockRequest entreeStockRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(entreeStockService.create(entreeStockRequest));
    }
}
