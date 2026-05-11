package org.store.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.PlanAbonnementRepository;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.PermissionsRepository;
import org.store.security.domain.repository.RoleRepository;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ROLE_PROPRIETAIRE = "PROPRIETAIRE";
    private static final String ROLE_EMPLOYE = "EMPLOYE";
    private static final String PERMISSION_PROPRIETAIRE_ACCESS = "PROPRIETAIRE_ACCESS";
    private static final String PERMISSION_EMPLOYE_ACCESS = "EMPLOYE_ACCESS";
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
        Permissions propAccess = ensurePermission(PERMISSION_PROPRIETAIRE_ACCESS);
        Permissions empAccess = ensurePermission(PERMISSION_EMPLOYE_ACCESS);

        ensureRole(ROLE_PROPRIETAIRE, "Propriétaire d'une entreprise", new LinkedHashSet<>(Set.of(propAccess)));
        ensureRole(ROLE_EMPLOYE, "Employé rattaché à un magasin", new LinkedHashSet<>(Set.of(empAccess)));

        ensureTrialPlan();
    }

    private Permissions ensurePermission(String code) {
        return permissionsRepository.findByCode(code).orElseGet(() -> {
            Permissions p = new Permissions();
            p.setCode(code);
            log.info("DataInitializer: création permission '{}'", code);
            return permissionsRepository.save(p);
        });
    }

    private void ensureRole(String libelle, String description, Set<Permissions> permissions) {
        roleRepository.findByLibelle(libelle).orElseGet(() -> {
            Role role = new Role();
            role.setLibelle(libelle);
            role.setDescription(description);
            role.setPermissions(permissions);
            log.info("DataInitializer: création rôle '{}'", libelle);
            return roleRepository.save(role);
        });
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
