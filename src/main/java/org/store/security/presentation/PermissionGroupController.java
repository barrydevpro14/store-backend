package org.store.security.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.security.application.dto.PermissionGroupResponse;
import org.store.security.application.service.IPermissionGroupService;

import java.util.List;

/**
 * Référentiel des groupes de permissions RBAC.
 * Retourne les groupes avec leurs permissions — utilisé pour alimenter
 * les dialogs de gestion des rôles sans traitement côté frontend.
 */
@RestController
@RequestMapping(PermissionGroupController.BASE_PATH)
public class PermissionGroupController {

    public static final String BASE_PATH = "/api/v1/permission-groups";

    private final IPermissionGroupService permissionGroupService;

    public PermissionGroupController(IPermissionGroupService permissionGroupService) {
        this.permissionGroupService = permissionGroupService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SYSTEM_ROLE_UPDATE')")
    public ResponseEntity<List<PermissionGroupResponse>> list() {
        return ResponseEntity.ok(permissionGroupService.findAll());
    }

    @GetMapping("/employee")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public ResponseEntity<List<PermissionGroupResponse>> listForCustomRole() {
        return ResponseEntity.ok(permissionGroupService.findAllForCustomRole());
    }
}
