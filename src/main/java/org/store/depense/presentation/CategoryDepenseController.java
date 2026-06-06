package org.store.depense.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.store.depense.application.dto.CategoryDepenseFilter;
import org.store.depense.application.dto.CategoryDepenseRequest;
import org.store.depense.application.dto.CategoryDepenseResponse;
import org.store.depense.application.service.ICategoryDepenseService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(CategoryDepenseController.BASE_PATH)
public class CategoryDepenseController {

    public static final String BASE_PATH = "/api/v1/categories-depense";

    private final ICategoryDepenseService categoryDepenseService;

    public CategoryDepenseController(ICategoryDepenseService categoryDepenseService) {
        this.categoryDepenseService = categoryDepenseService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EXPENSE_CREATE')")
    public ResponseEntity<CategoryDepenseResponse> create(@Valid @RequestBody CategoryDepenseRequest categoryDepenseRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryDepenseService.create(categoryDepenseRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EXPENSE_READ')")
    public ResponseEntity<Page<CategoryDepenseResponse>> list(@RequestParam(required = false) String nom,
                                                              @RequestParam(required = false) Boolean actif,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdStartDate,
                                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdEndDate,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(categoryDepenseService.findAll(
                new CategoryDepenseFilter(nom, actif, createdStartDate, createdEndDate, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_READ')")
    public ResponseEntity<CategoryDepenseResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryDepenseService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_UPDATE')")
    public ResponseEntity<CategoryDepenseResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody CategoryDepenseRequest categoryDepenseRequest) {
        return ResponseEntity.ok(categoryDepenseService.update(id, categoryDepenseRequest));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EXPENSE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryDepenseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
