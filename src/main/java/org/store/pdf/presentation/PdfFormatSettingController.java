package org.store.pdf.presentation;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.store.pdf.application.dto.PdfFormatSettingRequest;
import org.store.pdf.application.dto.PdfFormatSettingResponse;
import org.store.pdf.application.service.IPdfFormatSettingService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(PdfFormatSettingController.BASE_PATH)
public class PdfFormatSettingController {

    public static final String BASE_PATH = "/api/v1/pdf-format-settings";

    private final IPdfFormatSettingService service;

    public PdfFormatSettingController(IPdfFormatSettingService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PDF_FORMAT_SETTING_MANAGE')")
    public ResponseEntity<List<PdfFormatSettingResponse>> list(@RequestParam UUID magasinId) {
        return ResponseEntity.ok(service.findEffectiveForMagasin(magasinId));
    }

    @PutMapping("/{pdfFormatConfigId}")
    @PreAuthorize("hasAuthority('PDF_FORMAT_SETTING_MANAGE')")
    public ResponseEntity<PdfFormatSettingResponse> upsertOverride(@PathVariable UUID pdfFormatConfigId,
                                                                     @RequestParam UUID magasinId,
                                                                     @Valid @RequestBody PdfFormatSettingRequest pdfFormatSettingRequest) {
        return ResponseEntity.ok(service.upsertOverride(pdfFormatConfigId, magasinId, pdfFormatSettingRequest));
    }

    @DeleteMapping("/{pdfFormatConfigId}")
    @PreAuthorize("hasAuthority('PDF_FORMAT_SETTING_MANAGE')")
    public ResponseEntity<Void> deleteOverride(@PathVariable UUID pdfFormatConfigId, @RequestParam UUID magasinId) {
        service.deleteOverride(pdfFormatConfigId, magasinId);
        return ResponseEntity.noContent().build();
    }
}
