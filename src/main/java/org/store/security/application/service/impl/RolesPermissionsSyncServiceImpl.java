package org.store.security.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.RbacConfigException;
import org.store.property.RbacProperties;
import org.store.security.application.dto.RbacConfig;
import org.store.security.application.dto.RbacConfig.RoleDef;
import org.store.security.application.dto.RbacSyncReport;
import org.store.security.application.service.IRolesPermissionsSyncService;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.PermissionsDomainService;
import org.store.security.domain.service.RoleDomainService;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class RolesPermissionsSyncServiceImpl implements IRolesPermissionsSyncService {

    private static final Logger log = LoggerFactory.getLogger(RolesPermissionsSyncServiceImpl.class);

    private final RbacProperties rbacProperties;
    private final PermissionsDomainService permissionsDomainService;
    private final RoleDomainService roleDomainService;

    public RolesPermissionsSyncServiceImpl(RbacProperties rbacProperties,
                                           PermissionsDomainService permissionsDomainService,
                                           RoleDomainService roleDomainService) {
        this.rbacProperties = rbacProperties;
        this.permissionsDomainService = permissionsDomainService;
        this.roleDomainService = roleDomainService;
    }

    @Override
    @Transactional
    public RbacSyncReport sync() {
        RbacConfig config = loadConfig();

        Map<String, Permissions> catalog = new LinkedHashMap<>();
        List<String> addedPermissions = new ArrayList<>();
        for (String code : config.permissions()) {
            Permissions p = permissionsDomainService.findByCode(code).orElse(null);
            if (p == null) {
                Permissions created = new Permissions();
                created.setCode(code);
                p = permissionsDomainService.save(created);
                addedPermissions.add(code);
                log.info("RBAC sync: created permission '{}'", code);
            }
            catalog.put(code, p);
        }

        List<String> addedRoles = new ArrayList<>();
        List<String> updatedRoles = new ArrayList<>();
        for (RoleDef roleDef : config.roles()) {
            ensureRole(roleDef, catalog, addedRoles, updatedRoles);
        }

        List<String> orphanPermissions = permissionsDomainService.findAll().stream()
                .map(Permissions::getCode)
                .filter(code -> !catalog.containsKey(code))
                .toList();
        orphanPermissions.forEach(code ->
                log.warn("RBAC sync: orphan permission in DB but absent from YAML: '{}'", code));

        Set<String> yamlRoleLibelles = new LinkedHashSet<>();
        config.roles().forEach(r -> yamlRoleLibelles.add(r.libelle()));
        List<String> orphanRoles = roleDomainService.findAll().stream()
                .map(Role::getLibelle)
                .filter(libelle -> !yamlRoleLibelles.contains(libelle))
                .toList();
        orphanRoles.forEach(libelle ->
                log.warn("RBAC sync: orphan role in DB but absent from YAML: '{}'", libelle));

        log.info("RBAC sync completed: +{} permissions, +{} roles, {} role(s) updated, {} orphan permission(s), {} orphan role(s)",
                addedPermissions.size(), addedRoles.size(), updatedRoles.size(),
                orphanPermissions.size(), orphanRoles.size());

        return new RbacSyncReport(addedPermissions, addedRoles, updatedRoles, orphanPermissions, orphanRoles);
    }

    private void ensureRole(RoleDef roleDef,
                            Map<String, Permissions> catalog,
                            List<String> addedRoles,
                            List<String> updatedRoles) {
        Role role = roleDomainService.findByLibelle(roleDef.libelle()).orElse(null);
        boolean created = false;
        if (role == null) {
            Role newRole = new Role();
            newRole.setLibelle(roleDef.libelle());
            newRole.setDescription(roleDef.description());
            newRole.setPermissions(new LinkedHashSet<>());
            role = roleDomainService.save(newRole);
            addedRoles.add(roleDef.libelle());
            log.info("RBAC sync: created role '{}'", roleDef.libelle());
            created = true;
        }

        Set<Permissions> current = role.getPermissions();
        if (current == null) {
            current = new LinkedHashSet<>();
            role.setPermissions(current);
        }

        boolean associationsChanged = false;
        for (String code : roleDef.permissions()) {
            Permissions required = catalog.get(code);
            if (required == null) {
                throw new RbacConfigException("rbac.config.unknownPermission", roleDef.libelle(), code);
            }
            boolean alreadyPresent = current.stream()
                    .anyMatch(p -> Objects.equals(p.getId(), required.getId()));
            if (!alreadyPresent) {
                current.add(required);
                associationsChanged = true;
            }
        }

        Set<String> yamlCodes = new LinkedHashSet<>(roleDef.permissions());
        current.stream()
                .map(Permissions::getCode)
                .filter(code -> !yamlCodes.contains(code))
                .forEach(code -> log.warn(
                        "RBAC sync: role '{}' has permission '{}' in DB but not in YAML (kept)",
                        roleDef.libelle(), code));

        if (associationsChanged) {
            roleDomainService.save(role);
            if (!created) {
                updatedRoles.add(roleDef.libelle());
                log.info("RBAC sync: updated permissions of role '{}'", roleDef.libelle());
            }
        }
    }

    private RbacConfig loadConfig() {
        if (rbacProperties.file() == null || !rbacProperties.file().exists()) {
            throw new RbacConfigException("rbac.config.fileMissing", String.valueOf(rbacProperties.file()));
        }
        try (InputStream in = rbacProperties.file().getInputStream()) {
            Map<String, Object> root = new Yaml().load(in);
            if (root == null) {
                throw new RbacConfigException("rbac.config.fileEmpty", String.valueOf(rbacProperties.file()));
            }
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) root.getOrDefault("permissions", List.of());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rolesRaw = (List<Map<String, Object>>) root.getOrDefault("roles", List.of());
            List<RoleDef> roles = rolesRaw.stream()
                    .map(this::toRoleDef)
                    .toList();
            return new RbacConfig(permissions, roles);
        } catch (IOException e) {
            throw new RbacConfigException("rbac.config.loadFailed", e, String.valueOf(rbacProperties.file()));
        }
    }

    @SuppressWarnings("unchecked")
    private RoleDef toRoleDef(Map<String, Object> entry) {
        return new RoleDef(
                (String) entry.get("libelle"),
                (String) entry.get("description"),
                (List<String>) entry.getOrDefault("permissions", List.of())
        );
    }
}
