package org.store.depense.presentation;

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
import org.store.depense.application.dto.DepenseFilter;
import org.store.depense.application.dto.DepenseRequest;
import org.store.depense.application.dto.DepenseResponse;
import org.store.depense.application.dto.DepenseTotalResponse;
import org.store.depense.application.service.IDepenseService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(DepenseController.BASE_PATH)
public class DepenseController {

    public static final String BASE_PATH = "/api/v1/depenses";

    private final IDepenseService depenseService;

    public DepenseController(IDepenseService depenseService) {
        this.depenseService = depenseService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EXPENSE_CREATE')")
    public ResponseEntity<DepenseResponse> create(@Valid @RequestBody DepenseRequest depenseRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(depenseService.create(depenseRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EXPENSE_READ')")
    public ResponseEntity<Page<DepenseResponse>> list(@RequestParam UUID magasinId,
                                                      @RequestParam(required = false) UUID categoryId,
                                                      @RequestParam(required = false) String modePaiement,
                                                      @RequestParam(required = false) String startDate,
                                                      @RequestParam(required = false) String endDate,
                                                      @RequestParam(required = false) LocalDate createdStartDate,
                                                      @RequestParam(required = false) LocalDate createdEndDate,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(depenseService.findAllByCurrentEntreprise(
                new DepenseFilter(magasinId, categoryId, modePaiement, startDate, endDate,
                        createdStartDate, createdEndDate, page, size)
        ));
    }

    @GetMapping("/total")
    @PreAuthorize("hasAuthority('EXPENSE_READ')")
    public ResponseEntity<DepenseTotalResponse> computeTotal(@RequestParam UUID magasinId,
                                                             @RequestParam(required = false) UUID categoryId,
                                                             @RequestParam(required = false) String modePaiement,
                                                             @RequestParam(required = false) String startDate,
                                                             @RequestParam(required = false) String endDate,
                                                             @RequestParam(required = false) LocalDate createdStartDate,
                                                             @RequestParam(required = false) LocalDate createdEndDate) {
        return ResponseEntity.ok(depenseService.computeTotal(
                new DepenseFilter(magasinId, categoryId, modePaiement, startDate, endDate,
                        createdStartDate, createdEndDate, 0, 1)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_READ')")
    public ResponseEntity<DepenseResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(depenseService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_UPDATE')")
    public ResponseEntity<DepenseResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody DepenseRequest depenseRequest) {
        return ResponseEntity.ok(depenseService.update(id, depenseRequest));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        depenseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
