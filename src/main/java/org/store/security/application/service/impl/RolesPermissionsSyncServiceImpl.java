package org.store.security.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.RbacConfigException;
import org.store.property.RbacProperties;
import org.store.security.application.dto.RbacConfig;
import org.store.security.application.dto.RbacConfig.RoleDef;
import org.store.security.application.dto.RbacSyncContext;
import org.store.security.application.dto.RbacSyncReport;
import org.store.security.application.service.IRolesPermissionsSyncService;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.PermissionsDomainService;
import org.store.security.domain.service.RoleDomainService;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Synchronise les rôles et permissions du YAML ({@code security.rbac.file}) avec la base de données.
 * Stratégie additive : aucune suppression, les orphelins sont seulement loggués.
 */
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

    /** Exécute la synchronisation RBAC du YAML vers la base et retourne un rapport des entités ajoutées, mises à jour et orphelines. */
    @Override
    @Transactional
    public RbacSyncReport sync() {
        RbacConfig config = loadConfig();
        RbacSyncContext context = new RbacSyncContext();

        syncPermissions(config, context);
        syncRoles(config, context);

        List<String> orphanPermissions = findAndLogOrphanPermissions(context);
        List<String> orphanRoles = findAndLogOrphanRoles(config);

        logSyncCompleted(context, orphanPermissions, orphanRoles);
        return new RbacSyncReport(
                context.addedPermissions(), context.addedRoles(), context.updatedRoles(),
                orphanPermissions, orphanRoles
        );
    }

    /** Itère sur les permissions du YAML et garantit leur existence en BD via `ensurePermission`. */
    public void syncPermissions(RbacConfig config, RbacSyncContext context) {
        config.permissions().forEach(code -> ensurePermission(code, context));
    }

    /** Itère sur les rôles du YAML et garantit leur existence en BD avec les permissions listées via `ensureRole`. */
    public void syncRoles(RbacConfig config, RbacSyncContext context) {
        config.roles().forEach(roleDef -> ensureRole(roleDef, context));
    }

    /** Garantit qu'une permission du YAML existe en BD (création si absente) et l'enregistre dans le catalogue du context. */
    public void ensurePermission(String code, RbacSyncContext context) {
        Permissions permission = permissionsDomainService.findByCode(code)
                .orElseGet(() -> createPermission(code, context));

        context.catalog().put(code, permission);
    }

    /** Persiste une nouvelle Permission via le domain service et la trace dans `context.addedPermissions`. */
    public Permissions createPermission(String code, RbacSyncContext context) {
        Permissions saved = permissionsDomainService.create(code);
        context.addedPermissions().add(code);
        log.info("RBAC sync: created permission '{}'", code);
        return saved;
    }

    /** Garantit qu'un rôle du YAML existe en BD avec au moins les permissions listées, et alimente addedRoles/updatedRoles du context. */
    public void ensureRole(RoleDef roleDef, RbacSyncContext context) {
        Role existing = roleDomainService.findByLibelle(roleDef.libelle()).orElse(null);
        boolean wasCreated = (existing == null);
        Role role = wasCreated ? createRole(roleDef, context) : existing;

        boolean flagChanged = syncAssignableToEmployeFromYaml(roleDef, role);
        initRolePermissionsIfNeeded(role);
        boolean associationsChanged = attachPermissionsFromYaml(roleDef, role, context);
        logOrphanRolePermissions(roleDef, role);

        if ((associationsChanged || flagChanged) && !wasCreated) {
            roleDomainService.save(role);
            String libelle = roleDef.libelle();
            context.updatedRoles().add(libelle);
            log.info("RBAC sync: updated role '{}'", libelle);
        }
    }

    /** Persiste un nouveau Role via le domain service et le trace dans `context.addedRoles`. */
    public Role createRole(RoleDef roleDef, RbacSyncContext context) {
        Role saved = roleDomainService.create(
                roleDef.libelle(),
                roleDef.description(),
                roleDef.assignableToEmployeOrFalse()
        );
        String libelle = roleDef.libelle();
        context.addedRoles().add(libelle);
        log.info("RBAC sync: created role '{}'", libelle);
        return saved;
    }

    /**
     * Aligne le flag `assignableToEmploye` du rôle existant sur la valeur du
     * YAML (source de vérité). Retourne `true` si la valeur a changé — ce qui
     * déclenche un save dans `ensureRole`. Reste idempotent en l'absence de
     * diff.
     */
    public boolean syncAssignableToEmployeFromYaml(RoleDef roleDef, Role role) {
        boolean expected = roleDef.assignableToEmployeOrFalse();
        if (role.isAssignableToEmploye() == expected) return false;
        role.setAssignableToEmploye(expected);
        return true;
    }

    /** Initialise le set de permissions du rôle si null (évite NPE lors de l'association). */
    public void initRolePermissionsIfNeeded(Role role) {
        if (role.getPermissions() == null) {
            role.setPermissions(new LinkedHashSet<>());
        }
    }

    /** Attache toutes les permissions du YAML au rôle si absentes ; retourne `true` si au moins une association a été ajoutée. */
    public boolean attachPermissionsFromYaml(RoleDef roleDef, Role role, RbacSyncContext context) {
        Set<Permissions> current = role.getPermissions();

        long ajouts = roleDef.permissions().stream()
                .map(code -> resolvePermissionFromCatalog(roleDef, code, context))
                .filter(required -> attachPermissionIfAbsent(current, required))
                .count();

        return ajouts > 0;
    }

    /** Récupère la permission du catalogue ou lève `RbacConfigException` si elle est inconnue. */
    public Permissions resolvePermissionFromCatalog(RoleDef roleDef, String code, RbacSyncContext context) {
        Permissions required = context.catalog().get(code);
        if (required == null) {
            throw new RbacConfigException("rbac.config.unknownPermission", roleDef.libelle(), code);
        }
        return required;
    }

    /** Ajoute la permission au set du rôle si elle n'y est pas déjà ; retourne `true` si une nouvelle association a été créée. */
    public boolean attachPermissionIfAbsent(Set<Permissions> current, Permissions required) {
        boolean alreadyPresent = current.stream()
                .anyMatch(permission -> Objects.equals(permission.getId(), required.getId()));

        if (alreadyPresent) {
            return false;
        }
        current.add(required);
        return true;
    }

    /** Logge en WARN chaque permission présente en BD pour le rôle mais absente du YAML (conservée par stratégie additive). */
    public void logOrphanRolePermissions(RoleDef roleDef, Role role) {
        Set<String> yamlCodes = new LinkedHashSet<>(roleDef.permissions());

        role.getPermissions().stream()
                .map(Permissions::getCode)
                .filter(code -> !yamlCodes.contains(code))
                .forEach(code -> log.warn(
                        "RBAC sync: role '{}' has permission '{}' in DB but not in YAML (kept)",
                        roleDef.libelle(), code));
    }

    /** Calcule les permissions présentes en BD mais absentes du YAML, les logge et retourne la liste. */
    public List<String> findAndLogOrphanPermissions(RbacSyncContext context) {
        List<String> orphanPermissions = permissionsDomainService.findAll().stream()
                .map(Permissions::getCode)
                .filter(code -> !context.catalog().containsKey(code))
                .toList();

        orphanPermissions.forEach(code ->
                log.warn("RBAC sync: orphan permission in DB but absent from YAML: '{}'", code));

        return orphanPermissions;
    }

    /** Calcule les rôles présents en BD mais absents du YAML, les logge et retourne la liste. */
    public List<String> findAndLogOrphanRoles(RbacConfig config) {
        Set<String> yamlRoleLibelles = config.roles().stream()
                .map(RoleDef::libelle)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> orphanRoles = roleDomainService.findAll().stream()
                .map(Role::getLibelle)
                .filter(libelle -> !yamlRoleLibelles.contains(libelle))
                .toList();

        orphanRoles.forEach(libelle ->
                log.warn("RBAC sync: orphan role in DB but absent from YAML: '{}'", libelle));

        return orphanRoles;
    }

    /** Logge le rapport de fin de synchronisation (compteurs d'ajouts, mises à jour, orphelins). */
    public void logSyncCompleted(RbacSyncContext context, List<String> orphanPermissions, List<String> orphanRoles) {
        log.info("RBAC sync completed: +{} permissions, +{} roles, {} role(s) updated, {} orphan permission(s), {} orphan role(s)",
                context.addedPermissions().size(), context.addedRoles().size(), context.updatedRoles().size(),
                orphanPermissions.size(), orphanRoles.size());
    }

    /** Charge et parse le fichier YAML pointé par security.rbac.file. */
    public RbacConfig loadConfig() {
        validateConfigFileExists();

        try (InputStream in = rbacProperties.file().getInputStream()) {
            Map<String, Object> root = parseYamlRoot(in);
            List<String> permissions = extractPermissionsFromYaml(root);
            List<RoleDef> roles = extractRolesFromYaml(root);
            return new RbacConfig(permissions, roles);
        } catch (IOException e) {
            throw new RbacConfigException("rbac.config.loadFailed", e, String.valueOf(rbacProperties.file()));
        }
    }

    /** Lève `RbacConfigException("rbac.config.fileMissing")` si le fichier YAML est absent ou inaccessible. */
    public void validateConfigFileExists() {
        if (rbacProperties.file() == null || !rbacProperties.file().exists()) {
            throw new RbacConfigException("rbac.config.fileMissing", String.valueOf(rbacProperties.file()));
        }
    }

    /** Parse le YAML en Map racine et lève `RbacConfigException("rbac.config.fileEmpty")` si le contenu est vide. */
    public Map<String, Object> parseYamlRoot(InputStream in) {
        Map<String, Object> root = new Yaml().load(in);
        if (root == null) {
            throw new RbacConfigException("rbac.config.fileEmpty", String.valueOf(rbacProperties.file()));
        }
        return root;
    }

    /** Extrait la liste des codes de permission de la Map YAML racine (vide par défaut). */
    @SuppressWarnings("unchecked")
    public List<String> extractPermissionsFromYaml(Map<String, Object> root) {
        return (List<String>) root.getOrDefault("permissions", List.of());
    }

    /** Extrait la liste de RoleDef de la Map YAML racine, chaque entrée passant par `toRoleDef`. */
    @SuppressWarnings("unchecked")
    public List<RoleDef> extractRolesFromYaml(Map<String, Object> root) {
        List<Map<String, Object>> rolesRaw = (List<Map<String, Object>>) root.getOrDefault("roles", List.of());

        return rolesRaw.stream()
                .map(this::toRoleDef)
                .toList();
    }

    /** Convertit une entrée YAML brute (Map SnakeYAML) en RoleDef typé. `assignableToEmploye` est absent par défaut (= false). */
    @SuppressWarnings("unchecked")
    public RoleDef toRoleDef(Map<String, Object> entry) {
        return new RoleDef(
                (String) entry.get("libelle"),
                (String) entry.get("description"),
                (Boolean) entry.get("assignableToEmploye"),
                (List<String>) entry.getOrDefault("permissions", List.of())
        );
    }
}
