package org.store.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.PlanAbonnementRepository;
import org.store.security.application.enums.PermissionCode;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.PermissionsRepository;
import org.store.security.domain.repository.RoleRepository;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ROLE_PROPRIETAIRE = "PROPRIETAIRE";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_VENDEUR = "VENDEUR";
    private static final String ROLE_ADMIN = "ADMIN";

    private static final String PLAN_TRIAL_NOM = "Essai";

    private static final String[] AUTH_PERMISSIONS = {
            "AUTH_LOGIN", "AUTH_LOGOUT", "AUTH_REFRESH_TOKEN", "AUTH_RESET_PASSWORD", "AUTH_CHANGE_PASSWORD"
    };
    private static final String[] USER_PERMISSIONS = {
            "USER_CREATE", "USER_UPDATE", "USER_DELETE", "USER_READ", "USER_LOCK", "USER_UNLOCK", "USER_ASSIGN_ROLE"
    };
    private static final String[] COMPANY_PERMISSIONS = {
            "COMPANY_CREATE", "COMPANY_UPDATE", "COMPANY_READ", "COMPANY_DELETE"
    };
    private static final String[] STORE_PERMISSIONS = {
            "STORE_CREATE", "STORE_UPDATE", "STORE_DELETE", "STORE_READ", "STORE_ASSIGN_MANAGER"
    };
    private static final String[] PRODUCT_PERMISSIONS = {
            "PRODUCT_CREATE", "PRODUCT_UPDATE", "PRODUCT_DELETE", "PRODUCT_READ",
            "PRODUCT_IMPORT", "PRODUCT_EXPORT", "PRODUCT_UPLOAD_IMAGE"
    };
    private static final String[] STOCK_PERMISSIONS = {
            "STOCK_READ", "STOCK_ENTRY", "STOCK_EXIT", "STOCK_ADJUSTMENT", "STOCK_INVENTORY", "STOCK_TRANSFER"
    };
    private static final String[] PURCHASE_PERMISSIONS = {
            "PURCHASE_CREATE", "PURCHASE_UPDATE", "PURCHASE_DELETE", "PURCHASE_READ", "PURCHASE_APPROVE", "PURCHASE_PAY"
    };
    private static final String[] SALE_PERMISSIONS = {
            "SALE_CREATE", "SALE_UPDATE", "SALE_DELETE", "SALE_READ", "SALE_PAY", "SALE_CANCEL"
    };
    private static final String[] EXPENSE_PERMISSIONS = {
            "EXPENSE_CREATE", "EXPENSE_UPDATE", "EXPENSE_DELETE", "EXPENSE_READ", "EXPENSE_PAY"
    };
    private static final String[] PAYMENT_PERMISSIONS = {
            "PAYMENT_CREATE", "PAYMENT_READ", "PAYMENT_CANCEL", "PAYMENT_REFUND"
    };
    private static final String[] SUBSCRIPTION_PERMISSIONS = {
            "SUBSCRIPTION_CREATE", "SUBSCRIPTION_UPDATE", "SUBSCRIPTION_CANCEL", "SUBSCRIPTION_READ"
    };
    private static final String[] DOCUMENT_PERMISSIONS = {
            "DOCUMENT_UPLOAD", "DOCUMENT_READ", "DOCUMENT_DELETE", "DOCUMENT_DOWNLOAD"
    };
    private static final String[] DASHBOARD_PERMISSIONS = {
            "DASHBOARD_READ", "REPORT_EXPORT", "REPORT_FINANCIAL", "REPORT_STOCK", "REPORT_SALES"
    };
    private static final String[] SETTINGS_PERMISSIONS = {
            "SETTINGS_UPDATE", "SETTINGS_READ"
    };
    private static final String[] AUDIT_PERMISSIONS = {
            "AUDIT_READ", "AUDIT_EXPORT"
    };

    private static final String[][] ALL_GRANULAR_MODULES = {
            AUTH_PERMISSIONS, USER_PERMISSIONS, COMPANY_PERMISSIONS, STORE_PERMISSIONS,
            PRODUCT_PERMISSIONS, STOCK_PERMISSIONS, PURCHASE_PERMISSIONS, SALE_PERMISSIONS,
            EXPENSE_PERMISSIONS, PAYMENT_PERMISSIONS, SUBSCRIPTION_PERMISSIONS,
            DOCUMENT_PERMISSIONS, DASHBOARD_PERMISSIONS, SETTINGS_PERMISSIONS, AUDIT_PERMISSIONS
    };

    private final PermissionsRepository permissionsRepository;
    private final RoleRepository roleRepository;
    private final PlanAbonnementRepository planAbonnementRepository;

    public DataInitializer(PermissionsRepository permissionsRepository,
                           RoleRepository roleRepository,
                           PlanAbonnementRepository planAbonnementRepository) {
        this.permissionsRepository = permissionsRepository;
        this.roleRepository = roleRepository;
        this.planAbonnementRepository = planAbonnementRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, Permissions> catalog = new LinkedHashMap<>();
        for (PermissionCode legacy : PermissionCode.values()) {
            catalog.put(legacy.name(), ensurePermission(legacy.name()));
        }
        for (String[] module : ALL_GRANULAR_MODULES) {
            for (String code : module) {
                catalog.put(code, ensurePermission(code));
            }
        }

        ensureRole(ROLE_ADMIN, "Administrateur SaaS", resolveAll(catalog, catalog.keySet().toArray(new String[0])));

        ensureRole(ROLE_PROPRIETAIRE, "Propriétaire d'une entreprise", resolveAll(catalog, ownerPermissionCodes()));

        ensureRole(ROLE_MANAGER, "Manager d'un magasin", resolveAll(catalog, managerPermissionCodes()));

        ensureRole(ROLE_VENDEUR, "Vendeur d'un magasin", resolveAll(catalog, vendeurPermissionCodes()));

        ensureTrialPlan();
    }

    private String[] ownerPermissionCodes() {
        return Stream.of(
                AUTH_PERMISSIONS,
                USER_PERMISSIONS,
                new String[]{"COMPANY_READ", "COMPANY_UPDATE"},
                STORE_PERMISSIONS,
                PRODUCT_PERMISSIONS,
                STOCK_PERMISSIONS,
                PURCHASE_PERMISSIONS,
                SALE_PERMISSIONS,
                EXPENSE_PERMISSIONS,
                PAYMENT_PERMISSIONS,
                SUBSCRIPTION_PERMISSIONS,
                DOCUMENT_PERMISSIONS,
                DASHBOARD_PERMISSIONS,
                SETTINGS_PERMISSIONS,
                AUDIT_PERMISSIONS,
                new String[]{PermissionCode.PROPRIETAIRE_ACCESS.name(), PermissionCode.EMPLOYE_CREATE.name()}
        ).flatMap(Stream::of).toArray(String[]::new);
    }

    private String[] managerPermissionCodes() {
        return Stream.of(
                AUTH_PERMISSIONS,
                new String[]{"USER_READ", "USER_CREATE", "USER_UPDATE"},
                new String[]{"COMPANY_READ"},
                new String[]{"STORE_READ", "STORE_UPDATE"},
                PRODUCT_PERMISSIONS,
                STOCK_PERMISSIONS,
                PURCHASE_PERMISSIONS,
                SALE_PERMISSIONS,
                EXPENSE_PERMISSIONS,
                PAYMENT_PERMISSIONS,
                DOCUMENT_PERMISSIONS,
                new String[]{"DASHBOARD_READ", "REPORT_STOCK", "REPORT_SALES", "REPORT_FINANCIAL"},
                new String[]{PermissionCode.EMPLOYE_ACCESS.name(), PermissionCode.EMPLOYE_CREATE.name()}
        ).flatMap(Stream::of).toArray(String[]::new);
    }

    private String[] vendeurPermissionCodes() {
        return Stream.of(
                AUTH_PERMISSIONS,
                new String[]{"PRODUCT_READ"},
                new String[]{"STOCK_READ"},
                SALE_PERMISSIONS,
                new String[]{"PAYMENT_CREATE", "PAYMENT_READ"},
                new String[]{PermissionCode.EMPLOYE_ACCESS.name()}
        ).flatMap(Stream::of).toArray(String[]::new);
    }

    private Set<Permissions> resolveAll(Map<String, Permissions> catalog, String[] codes) {
        Set<Permissions> result = new LinkedHashSet<>();
        for (String code : codes) {
            Permissions p = catalog.get(code);
            if (p == null) {
                throw new IllegalStateException("DataInitializer: permission inconnue dans le catalog : " + code);
            }
            result.add(p);
        }
        return result;
    }

    private Permissions ensurePermission(String code) {
        return permissionsRepository.findByCode(code).orElseGet(() -> {
            Permissions p = new Permissions();
            p.setCode(code);
            log.info("DataInitializer: création permission '{}'", code);
            return permissionsRepository.save(p);
        });
    }

    private void ensureRole(String libelle, String description, Set<Permissions> requiredPermissions) {
        Role role = roleRepository.findByLibelle(libelle).orElseGet(() -> {
            Role created = new Role();
            created.setLibelle(libelle);
            created.setDescription(description);
            created.setPermissions(new LinkedHashSet<>());
            log.info("DataInitializer: création rôle '{}'", libelle);
            return roleRepository.save(created);
        });

        Set<Permissions> current = role.getPermissions();
        if (current == null) {
            current = new LinkedHashSet<>();
            role.setPermissions(current);
        }
        boolean changed = false;
        for (Permissions required : requiredPermissions) {
            boolean alreadyPresent = current.stream()
                    .anyMatch(p -> Objects.equals(p.getId(), required.getId()));
            if (!alreadyPresent) {
                current.add(required);
                changed = true;
            }
        }
        if (changed) {
            log.info("DataInitializer: synchronisation permissions du rôle '{}'", libelle);
            roleRepository.save(role);
        }
    }

    private void ensureTrialPlan() {
        planAbonnementRepository.findFirstByTrialTrueAndActifTrue().orElseGet(() -> {
            PlanAbonnement plan = new PlanAbonnement();
            plan.setNom(PLAN_TRIAL_NOM);
            plan.setDescription("Plan d'essai gratuit");
            plan.setPrix(BigDecimal.ZERO);
            plan.setNombreMagasinsMax(1);
            plan.setNombreEmployesMax(3);
            plan.setGestionStock(true);
            plan.setGestionVente(true);
            plan.setGestionAchat(true);
            plan.setGestionComptabilite(false);
            plan.setActif(true);
            plan.setVisible(true);
            plan.setTrial(true);
            plan.setOrdre(0);
            log.info("DataInitializer: création plan d'essai '{}'", PLAN_TRIAL_NOM);
            return planAbonnementRepository.save(plan);
        });
    }
}
