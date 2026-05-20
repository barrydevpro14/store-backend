package org.store.security.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.security.application.dto.PermissionResponse;
import org.store.security.application.service.IPermissionsService;

import java.util.List;

/**
 * Lecture seule du référentiel des permissions RBAC. Utile pour les UI
 * qui doivent peupler des selectors (rapport de rôle <-> permission,
 * filtres administrateurs, ...). Réservé aux utilisateurs authentifiés.
 */
@RestController
@RequestMapping(PermissionController.BASE_PATH)
public class PermissionController {

    public static final String BASE_PATH = "/api/v1/permissions";

    private final IPermissionsService permissionsService;

    public PermissionController(IPermissionsService permissionsService) {
        this.permissionsService = permissionsService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PermissionResponse>> list() {
        return ResponseEntity.ok(permissionsService.findAll());
    }
}
