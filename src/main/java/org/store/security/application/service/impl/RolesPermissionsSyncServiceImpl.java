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

/**
 * Synchronise les rôles et permissions définis dans le fichier YAML
 * (`security.rbac.file`, par défaut `classpath:security/roles-permissions.yml`)
 * avec la base de données.
 *
 * <p>Stratégie <b>additive</b> : on ne supprime jamais ; les éléments présents
 * en BD mais absents du YAML sont uniquement loggués en WARN (orphelins). Cela
 * évite qu'une mauvaise édition du YAML retire silencieusement des droits à des
 * utilisateurs en production.</p>
 *
 * <p>Idempotent : appeler {@link #sync()} N fois donne le même état final.</p>
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

    /**
     * Exécute la synchronisation en 4 étapes :
     * <ol>
     *   <li>Charger le YAML.</li>
     *   <li>Insérer les permissions manquantes (collecte dans un catalog
     *       en mémoire pour éviter les SELECTs lors de l'étape 3).</li>
     *   <li>Insérer ou mettre à jour chaque rôle (associations).</li>
     *   <li>Détecter les orphelins (permissions/rôles en BD absents du YAML)
     *       et les logger sans rien supprimer.</li>
     * </ol>
     * Toute l'opération est transactionnelle : en cas d'erreur, rien n'est commité.
     *
     * @return un rapport listant ce qui a été ajouté, mis à jour et signalé orphelin.
     */
    @Override
    @Transactional
    public RbacSyncReport sync() {
        // Étape 1 — chargement et parsing du YAML
        RbacConfig config = loadConfig();

        // Étape 2 — synchronisation des permissions.
        // Le catalog (Map<code, Permissions>) est rempli au fur et à mesure :
        // on l'utilise à l'étape 3 pour résoudre les associations role↔permission
        // sans refaire de SELECT par code.
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

        // Étape 3 — synchronisation des rôles + leurs associations.
        // Délégué à ensureRole(), qui gère création/mise à jour/log des orphelins par rôle.
        List<String> addedRoles = new ArrayList<>();
        List<String> updatedRoles = new ArrayList<>();
        for (RoleDef roleDef : config.roles()) {
            ensureRole(roleDef, catalog, addedRoles, updatedRoles);
        }

        // Étape 4 — détection des orphelins globaux (en BD, pas dans le YAML).
        // On compare l'état BD à la déclaration YAML ; rien n'est supprimé,
        // on se contente de prévenir l'opérateur.
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

    /**
     * Garantit qu'un rôle déclaré dans le YAML existe en BD avec au moins
     * toutes les permissions listées.
     *
     * <p>Trois cas :</p>
     * <ul>
     *   <li><b>Rôle absent</b> → création avec ses permissions, ajouté à {@code addedRoles}.</li>
     *   <li><b>Rôle existant, associations à compléter</b> → on ajoute les permissions
     *       manquantes, le rôle est ajouté à {@code updatedRoles}.</li>
     *   <li><b>Rôle existant, déjà à jour</b> → aucune écriture, aucune trace dans le rapport.</li>
     * </ul>
     *
     * <p>Les permissions associées en BD mais absentes du YAML pour ce rôle sont
     * loggées en WARN puis <b>conservées</b> (stratégie additive — voir la classe).</p>
     *
     * @throws RbacConfigException si le YAML référence une permission qui n'est pas
     *         déclarée dans la section globale {@code permissions} (incohérence).
     */
    private void ensureRole(RoleDef roleDef,
                            Map<String, Permissions> catalog,
                            List<String> addedRoles,
                            List<String> updatedRoles) {
        // Recherche par libellé (clé fonctionnelle du rôle, pas l'UUID).
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

        // Garde-fou : si la collection lazy a été initialisée à null côté JPA.
        Set<Permissions> current = role.getPermissions();
        if (current == null) {
            current = new LinkedHashSet<>();
            role.setPermissions(current);
        }

        // Ajout des associations manquantes — on s'appuie sur le catalog (étape 2)
        // pour récupérer l'entité Permissions déjà persistée. La comparaison par
        // ID évite les faux négatifs liés à equals/hashCode des entités JPA.
        boolean associationsChanged = false;
        for (String code : roleDef.permissions()) {
            Permissions required = catalog.get(code);
            if (required == null) {
                // Le YAML est incohérent : un rôle référence une permission
                // qui n'est pas déclarée dans la section globale `permissions`.
                throw new RbacConfigException("rbac.config.unknownPermission", roleDef.libelle(), code);
            }
            boolean alreadyPresent = current.stream()
                    .anyMatch(p -> Objects.equals(p.getId(), required.getId()));
            if (!alreadyPresent) {
                current.add(required);
                associationsChanged = true;
            }
        }

        // Orphelins par rôle : permissions en BD mais absentes du YAML pour ce rôle.
        // On log mais on ne retire rien (stratégie additive).
        Set<String> yamlCodes = new LinkedHashSet<>(roleDef.permissions());
        current.stream()
                .map(Permissions::getCode)
                .filter(code -> !yamlCodes.contains(code))
                .forEach(code -> log.warn(
                        "RBAC sync: role '{}' has permission '{}' in DB but not in YAML (kept)",
                        roleDef.libelle(), code));

        // On ne persiste que si quelque chose a vraiment changé.
        // `updatedRoles` ne contient pas les rôles fraîchement créés (déjà dans `addedRoles`).
        if (associationsChanged) {
            roleDomainService.save(role);
            if (!created) {
                updatedRoles.add(roleDef.libelle());
                log.info("RBAC sync: updated permissions of role '{}'", roleDef.libelle());
            }
        }
    }

    /**
     * Charge et parse le fichier YAML pointé par {@code security.rbac.file}.
     *
     * @throws RbacConfigException si la ressource est introuvable
     *         ({@code rbac.config.fileMissing}), vide ({@code rbac.config.fileEmpty})
     *         ou illisible ({@code rbac.config.loadFailed}).
     */
    private RbacConfig loadConfig() {
        if (rbacProperties.file() == null || !rbacProperties.file().exists()) {
            throw new RbacConfigException("rbac.config.fileMissing", String.valueOf(rbacProperties.file()));
        }
        try (InputStream in = rbacProperties.file().getInputStream()) {
            // SnakeYAML est inclus transitivement par Spring Boot (sert à parser
            // application.yml) — pas de dépendance Maven supplémentaire requise.
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

    /**
     * Convertit une entrée brute du YAML (Map issu de SnakeYAML) en {@link RoleDef}.
     * Le cast est sûr tant que le schéma YAML est respecté ; en cas d'erreur de
     * schéma, une {@code ClassCastException} sera levée — c'est volontairement
     * fail-fast, le YAML doit être correct au boot.
     */
    @SuppressWarnings("unchecked")
    private RoleDef toRoleDef(Map<String, Object> entry) {
        return new RoleDef(
                (String) entry.get("libelle"),
                (String) entry.get("description"),
                (List<String>) entry.getOrDefault("permissions", List.of())
        );
    }
}
