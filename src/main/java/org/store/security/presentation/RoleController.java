package org.store.security.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.store.security.application.dto.RoleListResponse;
import org.store.security.application.dto.RoleRequest;
import org.store.security.application.dto.RoleResponse;
import org.store.security.application.dto.RoleUpdateRequest;
import org.store.security.application.service.IRoleService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(RoleController.BASE_PATH)
public class RoleController {

    public static final String BASE_PATH = "/api/v1/roles";

    private final IRoleService roleService;

    public RoleController(IRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/system")
    @PreAuthorize("hasAuthority('SYSTEM_ROLE_UPDATE')")
    public ResponseEntity<List<RoleListResponse>> listSystem() {
        return ResponseEntity.ok(roleService.findAllSystem());
    }

    @GetMapping("/employee")
    @PreAuthorize("hasAuthority('USER_ASSIGN_ROLE')")
    public ResponseEntity<List<RoleListResponse>> listEmployee() {
        return ResponseEntity.ok(roleService.findAllEmployee());
    }

    @GetMapping("/company")
    @PreAuthorize("hasAuthority('USER_ASSIGN_ROLE')")
    public ResponseEntity<List<RoleListResponse>> listForManagement() {
        return ResponseEntity.ok(roleService.findAllForManagement());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RoleResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.findByIdWithPermissions(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public ResponseEntity<RoleResponse> createCustomRole(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createCustomRole(request));
    }

    @PostMapping("/system")
    @PreAuthorize("hasAuthority('SYSTEM_ROLE_UPDATE')")
    public ResponseEntity<RoleResponse> createSystemRole(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createSystemRole(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<RoleResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(roleService.update(id, request));
    }

    @PatchMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<RoleResponse> updatePermissions(@PathVariable UUID id,
                                                          @RequestBody List<String> permissionCodes) {
        return ResponseEntity.ok(roleService.updatePermissions(id, permissionCodes));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<RoleResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<RoleResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.deactivate(id));
    }

    @PatchMapping("/{id}/system")
    @PreAuthorize("hasAuthority('SYSTEM_ROLE_UPDATE')")
    public ResponseEntity<RoleResponse> updateSystemRole(@PathVariable UUID id,
                                                         @Valid @RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(roleService.updateSystemRole(id, request));
    }

    @PatchMapping("/{id}/system/permissions")
    @PreAuthorize("hasAuthority('SYSTEM_ROLE_UPDATE')")
    public ResponseEntity<RoleResponse> updateSystemRolePermissions(@PathVariable UUID id,
                                                                    @RequestBody List<String> permissionCodes) {
        return ResponseEntity.ok(roleService.updateSystemRolePermissions(id, permissionCodes));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
