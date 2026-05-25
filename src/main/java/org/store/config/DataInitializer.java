package org.store.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.PlanAbonnementRequest;
import org.store.abonnement.application.dto.SubscriptionTypeRequest;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.PlanAbonnementDomainService;
import org.store.abonnement.domain.service.TypePlanAbonnementDomainService;
import org.store.property.RbacProperties;
import org.store.security.application.service.IRolesPermissionsSyncService;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.AccountDomainService;
import org.store.security.domain.service.RoleDomainService;
import org.store.security.domain.model.Account;
import org.store.users.domain.model.Utilisateur;
import org.store.users.domain.service.UtilisateurDomainService;

import java.math.BigDecimal;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String PLAN_TRIAL_NOM = "Essai";
    private static final String TYPE_TRIAL_NOM = "Essai";

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_ROLE = "ADMIN";

    private final RbacProperties rbacProperties;
    private final IRolesPermissionsSyncService rolesPermissionsSyncService;
    private final PlanAbonnementDomainService planAbonnementDomainService;
    private final TypePlanAbonnementDomainService typePlanAbonnementDomainService;
    private final AccountDomainService accountDomainService;
    private final RoleDomainService roleDomainService;
    private final PasswordEncoder passwordEncoder;
    private final UtilisateurDomainService utilisateurDomainService;

    public DataInitializer(RbacProperties rbacProperties,
                           IRolesPermissionsSyncService rolesPermissionsSyncService,
                           PlanAbonnementDomainService planAbonnementDomainService,
                           TypePlanAbonnementDomainService typePlanAbonnementDomainService,
                           AccountDomainService accountDomainService,
                           RoleDomainService roleDomainService,
                           PasswordEncoder passwordEncoder,
                           UtilisateurDomainService utilisateurDomainService) {
        this.rbacProperties = rbacProperties;
        this.rolesPermissionsSyncService = rolesPermissionsSyncService;
        this.planAbonnementDomainService = planAbonnementDomainService;
        this.typePlanAbonnementDomainService = typePlanAbonnementDomainService;
        this.accountDomainService = accountDomainService;
        this.roleDomainService = roleDomainService;
        this.passwordEncoder = passwordEncoder;
        this.utilisateurDomainService = utilisateurDomainService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (rbacProperties.sync()) {
            rolesPermissionsSyncService.sync();
            ensureAdminAccount();
        } else {
            log.info("DataInitializer: RBAC sync skipped (security.rbac.sync=false)");
        }
        ensureTrialPlan();
    }

    /**
     * Idempotent : crée le compte ADMIN seedé s'il n'existe pas encore, puis
     * s'assure qu'un profil Utilisateur lui est attaché (pour que GET /users/me
     * ne retourne pas 500 sur l'admin seedé). Chaque étape est indépendante :
     * un redémarrage avec un compte déjà existant mais sans Utilisateur rattrapera
     * le profil manquant.
     */
    private void ensureAdminAccount() {
        Account adminAccount = accountDomainService.findByUsername(ADMIN_USERNAME).orElse(null);

        if (adminAccount == null) {
            Role adminRole = roleDomainService.findByLibelle(ADMIN_ROLE)
                    .orElseThrow(() -> new IllegalStateException(
                            "Rôle ADMIN absent en base — la sync RBAC doit s'exécuter avant ensureAdminAccount."));
            adminAccount = accountDomainService.create(
                    ADMIN_USERNAME, passwordEncoder.encode(rbacProperties.adminPassword()), adminRole);
            log.info("DataInitializer: compte ADMIN seedé (username={})", ADMIN_USERNAME);
        }

        Account finalAdminAccount = adminAccount;
        boolean hasProfile = utilisateurDomainService.findByAccountId(adminAccount.getId()).isPresent();
        if (!hasProfile) {
            Utilisateur utilisateur = new Utilisateur();
            utilisateur.setAccount(finalAdminAccount);
            utilisateur.setNom("Admin");
            utilisateur.setPrenom("Système");
            utilisateurDomainService.save(utilisateur);
            log.info("DataInitializer: profil Utilisateur créé pour le compte ADMIN seedé");
        }
    }

    private void ensureTrialPlan() {
        PlanAbonnement trialPlan = planAbonnementDomainService.findFirstTrialActif().orElse(null);

        if (trialPlan == null) {
            trialPlan = planAbonnementDomainService.create(new PlanAbonnementRequest(
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

        ensureTrialPlanHasDefaultType(trialPlan);
    }

    /**
     * Seeds the default {@code TypePlanAbonnement} that the signup flow binds the TRIAL Abonnement to.
     * Marked {@code trial=true} so {@code TypePlanAbonnementDomainService.findFirstActifTrial()} picks it up.
     */
    private void ensureTrialPlanHasDefaultType(PlanAbonnement trialPlan) {
        if (typePlanAbonnementDomainService.existsByPlanIdAndNom(trialPlan.getId(), TYPE_TRIAL_NOM)) {
            return;
        }
        var trialType = typePlanAbonnementDomainService.create(trialPlan, new SubscriptionTypeRequest(
                TYPE_TRIAL_NOM,
                1,
                null,
                null,
                false,
                true,
                0
        ));
        trialType.setTrial(true);
        typePlanAbonnementDomainService.save(trialType);
        log.info("DataInitializer: création type d'essai '{}' sur le plan '{}'", TYPE_TRIAL_NOM, trialPlan.getNom());
    }
}
