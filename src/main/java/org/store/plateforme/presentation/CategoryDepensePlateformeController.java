package org.store.plateforme.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.store.common.dto.DataSelect;
import org.store.plateforme.application.dto.CategoryDepensePlateformeFilter;
import org.store.plateforme.application.dto.CategoryDepensePlateformeRequest;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.application.service.ICategoryDepensePlateformeService;

import java.util.UUID;

@RestController
@RequestMapping(CategoryDepensePlateformeController.BASE_PATH)
public class CategoryDepensePlateformeController {

    public static final String BASE_PATH = "/api/v1/admin/plateforme/expense-categories";

    private final ICategoryDepensePlateformeService service;

    public CategoryDepensePlateformeController(ICategoryDepensePlateformeService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_CATEGORY_CREATE')")
    public ResponseEntity<CategoryDepensePlateformeResponse> create(@Valid @RequestBody CategoryDepensePlateformeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_CATEGORY_READ')")
    public ResponseEntity<Page<CategoryDepensePlateformeResponse>> list(@RequestParam(required = false) String nom,
                                                                        @RequestParam(required = false) Boolean actif,
                                                                        @RequestParam(required = false) String startDate,
                                                                        @RequestParam(required = false) String endDate,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.findAll(new CategoryDepensePlateformeFilter(nom, actif, startDate, endDate, page, size)));
    }

    @GetMapping("/select")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_CATEGORY_READ')")
    public ResponseEntity<Page<DataSelect>> select(@RequestParam(required = false) String q,
                                                                             @RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.findSelectItems(q, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_CATEGORY_READ')")
    public ResponseEntity<CategoryDepensePlateformeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_CATEGORY_UPDATE')")
    public ResponseEntity<CategoryDepensePlateformeResponse> update(@PathVariable UUID id,
                                                                    @Valid @RequestBody CategoryDepensePlateformeRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_CATEGORY_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
