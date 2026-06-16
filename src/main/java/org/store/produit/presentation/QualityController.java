package org.store.produit.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.produit.application.dto.QualityFilter;
import org.store.produit.application.dto.QualityRequest;
import org.store.produit.application.dto.QualityResponse;
import org.store.produit.application.service.IQualityService;

import java.util.UUID;

@RestController
@RequestMapping(QualityController.BASE_PATH)
public class QualityController {

    public static final String BASE_PATH = "/api/v1/qualities";

    private final IQualityService qualityService;

    public QualityController(IQualityService qualityService) {
        this.qualityService = qualityService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('QUALITY_CREATE')")
    public ResponseEntity<QualityResponse> create(@Valid @RequestBody QualityRequest qualityRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(qualityService.create(qualityRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('QUALITY_READ')")
    public ResponseEntity<Page<QualityResponse>> list(@RequestParam(required = false) String libelle,
                                                      @RequestParam(required = false) String startDate,
                                                      @RequestParam(required = false) String endDate,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(qualityService.findAll(
                new QualityFilter(libelle, startDate, endDate, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('QUALITY_READ')")
    public ResponseEntity<QualityResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(qualityService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('QUALITY_UPDATE')")
    public ResponseEntity<QualityResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody QualityRequest qualityRequest) {
        return ResponseEntity.ok(qualityService.update(id, qualityRequest));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('QUALITY_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        qualityService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
