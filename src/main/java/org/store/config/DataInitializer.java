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
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ROLE_PROPRIETAIRE = "PROPRIETAIRE";
    private static final String ROLE_MANAGER = "MANAGER";
    private static final String ROLE_VENDEUR = "VENDEUR";
    private static final String ROLE_ADMIN = "ADMIN";

    private static final String PLAN_TRIAL_NOM = "Essai";

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
        Permissions propAccess = ensurePermission(PermissionCode.PROPRIETAIRE_ACCESS);
        Permissions empAccess = ensurePermission(PermissionCode.EMPLOYE_ACCESS);
        Permissions empCreate = ensurePermission(PermissionCode.EMPLOYE_CREATE);
        Permissions adminAccess = ensurePermission(PermissionCode.ADMIN_ACCESS);

        ensureRole(ROLE_PROPRIETAIRE, "Propriétaire d'une entreprise", Set.of(propAccess, empCreate));
        ensureRole(ROLE_MANAGER, "Manager d'un magasin", Set.of(empAccess, empCreate));
        ensureRole(ROLE_VENDEUR, "Vendeur d'un magasin", Set.of(empAccess));
        ensureRole(ROLE_ADMIN, "Administrateur SaaS", Set.of(adminAccess));

        ensureTrialPlan();
    }

    private Permissions ensurePermission(PermissionCode permissionCode) {
        String code = permissionCode.name();
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
