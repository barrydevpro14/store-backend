package org.store.security.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.security.application.dto.RoleResponse;
import org.store.security.application.service.IRoleService;

import java.util.List;

/**
 * Lecture seule du référentiel des rôles RBAC. Sert principalement les
 * UI qui doivent peupler des selectors (création / édition d'employé).
 * Tous les utilisateurs authentifiés peuvent consulter la liste — le
 * filtrage "rôles assignables par le caller" reste géré par le frontend
 * sur la base de `permissions` côté JWT, et le backend vérifie au write.
 */
@RestController
@RequestMapping(RoleController.BASE_PATH)
public class RoleController {

    public static final String BASE_PATH = "/api/v1/roles";

    private final IRoleService roleService;

    public RoleController(IRoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RoleResponse>> list() {
        return ResponseEntity.ok(roleService.findAll());
    }
}
