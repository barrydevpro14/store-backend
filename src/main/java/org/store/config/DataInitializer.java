package org.store.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.PlanAbonnementRepository;
import org.store.property.RbacProperties;
import org.store.security.application.service.IRolesPermissionsSyncService;

import java.math.BigDecimal;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String PLAN_TRIAL_NOM = "Essai";

    private final RbacProperties rbacProperties;
    private final IRolesPermissionsSyncService rolesPermissionsSyncService;
    private final PlanAbonnementRepository planAbonnementRepository;

    public DataInitializer(RbacProperties rbacProperties,
                           IRolesPermissionsSyncService rolesPermissionsSyncService,
                           PlanAbonnementRepository planAbonnementRepository) {
        this.rbacProperties = rbacProperties;
        this.rolesPermissionsSyncService = rolesPermissionsSyncService;
        this.planAbonnementRepository = planAbonnementRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (rbacProperties.sync()) {
            rolesPermissionsSyncService.sync();
        } else {
            log.info("DataInitializer: RBAC sync skipped (security.rbac.sync=false)");
        }
        ensureTrialPlan();
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
