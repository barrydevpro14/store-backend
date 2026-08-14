package org.store.pdf.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.pdf.application.dto.PdfFormatConfigResponse;
import org.store.pdf.application.service.IPdfFormatConfigService;

import java.util.List;

@RestController
@RequestMapping(PdfFormatConfigController.BASE_PATH)
public class PdfFormatConfigController {

    public static final String BASE_PATH = "/api/v1/pdf-configs";

    private final IPdfFormatConfigService service;

    public PdfFormatConfigController(IPdfFormatConfigService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PdfFormatConfigResponse>> list() {
        return ResponseEntity.ok(service.findAll());
    }
}
