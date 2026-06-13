package org.store.users.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;
import org.store.security.application.dto.ResetPasswordRequest;
import org.store.users.application.dto.AssignRoleRequest;
import org.store.users.application.dto.EmployeFilter;
import org.store.users.application.dto.EmployeRequest;
import org.store.users.application.dto.EmployeResponse;
import org.store.users.application.dto.EmployeUpdateRequest;
import org.store.users.application.service.IEmployeService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(EmployeController.BASE_PATH)
public class EmployeController {

    public static final String BASE_PATH = "/api/v1/employees";

    private final IEmployeService employeService;

    public EmployeController(IEmployeService employeService) {
        this.employeService = employeService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYE_CREATE')")
    public ResponseEntity<EmployeResponse> create(@Valid @RequestBody EmployeRequest employeRequest) {
        EmployeResponse response = employeService.create(employeRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYE_READ')")
    public ResponseEntity<Page<EmployeResponse>> list(@RequestParam(required = false) String nom,
                                                      @RequestParam(required = false) String prenom,
                                                      @RequestParam(required = false) String role,
                                                      @RequestParam(required = false) UUID magasinId,
                                                      @RequestParam(required = false) Boolean actif,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdStartDate,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdEndDate,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(employeService.findAllByCurrentEntreprise(
                new EmployeFilter(nom, prenom, role, magasinId, actif, createdStartDate, createdEndDate, page, size)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYE_READ')")
    public ResponseEntity<EmployeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(employeService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYE_UPDATE')")
    public ResponseEntity<EmployeResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody EmployeUpdateRequest request) {
        return ResponseEntity.ok(employeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYE_DELETE')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        employeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('EMPLOYE_DELETE')")
    public ResponseEntity<Void> activate(@PathVariable UUID id) {
        employeService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAuthority('USER_ASSIGN_ROLE')")
    public ResponseEntity<EmployeResponse> assignRole(@PathVariable UUID id,
                                                      @Valid @RequestBody AssignRoleRequest request) {
        return ResponseEntity.ok(employeService.assignRole(id, request));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('EMPLOYE_RESET_PASSWORD')")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id,
                                              @Valid @RequestBody ResetPasswordRequest request) {
        employeService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasAuthority('EMPLOYE_PURGE')")
    public ResponseEntity<Void> permanentDelete(@PathVariable UUID id) {
        employeService.permanentDelete(id);
        return ResponseEntity.noContent().build();
    }
}
