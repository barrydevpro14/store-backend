package org.store.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.PlanAbonnementRequest;
import org.store.abonnement.domain.service.PlanAbonnementDomainService;
import org.store.property.RbacProperties;
import org.store.security.application.service.IRolesPermissionsSyncService;

import java.math.BigDecimal;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String PLAN_TRIAL_NOM = "Essai";

    private final RbacProperties rbacProperties;
    private final IRolesPermissionsSyncService rolesPermissionsSyncService;
    private final PlanAbonnementDomainService planAbonnementDomainService;

    public DataInitializer(RbacProperties rbacProperties,
                           IRolesPermissionsSyncService rolesPermissionsSyncService,
                           PlanAbonnementDomainService planAbonnementDomainService) {
        this.rbacProperties = rbacProperties;
        this.rolesPermissionsSyncService = rolesPermissionsSyncService;
        this.planAbonnementDomainService = planAbonnementDomainService;
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
        if (planAbonnementDomainService.findFirstTrialActif().isPresent()) {
            return;
        }

        planAbonnementDomainService.create(new PlanAbonnementRequest(
                PLAN_TRIAL_NOM,
                "Plan d'essai gratuit",
                BigDecimal.ZERO,
                1,
                3,
                true,
                true,
                true,
                false,
                true,
                true,
                true,
                0
        ));
        log.info("DataInitializer: création plan d'essai '{}'", PLAN_TRIAL_NOM);
    }
}
