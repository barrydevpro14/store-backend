package org.store.entreprise.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ImageDownloadResponse;
import org.store.entreprise.application.dto.EntrepriseFilter;
import org.store.entreprise.application.dto.EntrepriseRequest;
import org.store.entreprise.application.dto.EntrepriseResponse;
import org.store.entreprise.application.dto.EntrepriseStatsResponse;
import org.store.entreprise.application.service.IEntrepriseService;

import java.util.List;
import org.store.security.application.dto.RegisterPropertyRequest;
import org.store.security.application.service.IRegisterPropertyService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(EntrepriseController.BASE_PATH)
public class EntrepriseController {

    public static final String BASE_PATH = "/api/v1/entreprises";

    private final IEntrepriseService entrepriseService;
    private final IRegisterPropertyService registerPropertyService;

    public EntrepriseController(IEntrepriseService entrepriseService,
                                IRegisterPropertyService registerPropertyService) {
        this.entrepriseService = entrepriseService;
        this.registerPropertyService = registerPropertyService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COMPANY_CREATE')")
    public ResponseEntity<EntrepriseResponse> create(@Valid @RequestBody RegisterPropertyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registerPropertyService.registerEntrepriseByAdmin(request));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('COMPANY_READ')")
    public ResponseEntity<List<EntrepriseStatsResponse>> stats() {
        return ResponseEntity.ok(entrepriseService.findStats());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('COMPANY_READ')")
    public ResponseEntity<Page<EntrepriseResponse>> list(@RequestParam(required = false) String sigle,
                                                         @RequestParam(required = false) String raisonSociale,
                                                         @RequestParam(required = false) String ninea,
                                                         @RequestParam(required = false) String rccm,
                                                         @RequestParam(required = false) Boolean actif,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdStartDate,
                                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdEndDate,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(entrepriseService.findAll(
                new EntrepriseFilter(sigle, raisonSociale, ninea, rccm, actif, createdStartDate, createdEndDate, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPANY_READ')")
    public ResponseEntity<EntrepriseResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(entrepriseService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COMPANY_UPDATE')")
    public ResponseEntity<EntrepriseResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody EntrepriseRequest request) {
        return ResponseEntity.ok(entrepriseService.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('COMPANY_UPDATE')")
    public ResponseEntity<EntrepriseResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(entrepriseService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('COMPANY_UPDATE')")
    public ResponseEntity<EntrepriseResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(entrepriseService.deactivate(id));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('COMPANY_READ')")
    public ResponseEntity<EntrepriseResponse> getMine() {
        return ResponseEntity.ok(entrepriseService.findCurrentUserEntreprise());
    }

    @PutMapping("/me")
    @PreAuthorize("hasAuthority('COMPANY_UPDATE')")
    public ResponseEntity<EntrepriseResponse> updateMine(@Valid @RequestBody EntrepriseRequest entrepriseRequest) {
        return ResponseEntity.ok(entrepriseService.updateCurrentUserEntreprise(entrepriseRequest));
    }

    @PutMapping(value = "/me/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('COMPANY_UPDATE')")
    public ResponseEntity<EntrepriseResponse> uploadLogo(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(entrepriseService.uploadCurrentUserLogo(file));
    }

    @GetMapping("/me/logo")
    @PreAuthorize("hasAuthority('COMPANY_READ')")
    public ResponseEntity<byte[]> getLogo() {
        ImageDownloadResponse download = entrepriseService.getCurrentUserLogo();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.content());
    }

    @DeleteMapping("/me/logo")
    @PreAuthorize("hasAuthority('COMPANY_UPDATE')")
    public ResponseEntity<Void> deleteLogo() {
        entrepriseService.deleteCurrentUserLogo();
        return ResponseEntity.noContent().build();
    }
}
