package org.store.stock.presentation;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.stock.application.dto.MouvementStockFilter;
import org.store.stock.application.dto.MouvementStockResponse;
import org.store.stock.application.service.IMouvementStockService;

import java.util.UUID;

@RestController
@RequestMapping(MouvementStockController.BASE_PATH)
@Validated
public class MouvementStockController {

    public static final String BASE_PATH = "/api/v1/stock-movements";

    private final IMouvementStockService mouvementStockService;

    public MouvementStockController(IMouvementStockService mouvementStockService) {
        this.mouvementStockService = mouvementStockService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_READ')")
    public ResponseEntity<Page<MouvementStockResponse>> list(@RequestParam UUID magasinId,
                                                             @RequestParam(required = false) UUID stockId,
                                                             @RequestParam(required = false) UUID productId,
                                                             @RequestParam(required = false) String type,
                                                             @RequestParam(required = false) String startDate,
                                                             @RequestParam(required = false) String endDate,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(mouvementStockService.findAllByCurrentEntreprise(
                new MouvementStockFilter(magasinId, productId, stockId, type, startDate, endDate, page, size)
        ));
    }
}
