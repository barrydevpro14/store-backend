package org.store.security.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.security.application.dto.AdminAccountRequest;
import org.store.security.application.dto.AdminAccountResponse;
import org.store.security.application.service.IAdminAccountService;

import java.util.UUID;

@RestController
@RequestMapping(AdminAccountController.BASE_PATH)
public class AdminAccountController {

    public static final String BASE_PATH = "/api/v1/admin-accounts";

    private final IAdminAccountService adminAccountService;

    public AdminAccountController(IAdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<Page<AdminAccountResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminAccountService.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<AdminAccountResponse> create(@Valid @RequestBody AdminAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminAccountService.create(request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('USER_UNLOCK')")
    public ResponseEntity<AdminAccountResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(adminAccountService.setEnabled(id, true));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('USER_LOCK')")
    public ResponseEntity<AdminAccountResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(adminAccountService.setEnabled(id, false));
    }
}
