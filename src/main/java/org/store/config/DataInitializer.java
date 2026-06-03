package org.store.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.PlanAbonnementRequest;
import org.store.abonnement.application.dto.SubscriptionTypeRequest;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.PlanAbonnementDomainService;
import org.store.abonnement.domain.service.TypePlanAbonnementDomainService;
import org.store.notification.domain.enums.CanalNotification;
import org.store.notification.domain.enums.NotificationStatut;
import org.store.notification.domain.model.Notification;
import org.store.notification.domain.service.NotificationDomainService;
import org.store.property.RbacProperties;
import org.store.security.application.service.IRolesPermissionsSyncService;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.AccountDomainService;
import org.store.security.domain.service.RoleDomainService;
import org.store.security.domain.model.Account;
import org.store.users.domain.model.Utilisateur;
import org.store.users.domain.service.UtilisateurDomainService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private final NotificationDomainService notificationDomainService;
    private final DemoProductSeeder demoProductSeeder;

    public DataInitializer(RbacProperties rbacProperties,
                           IRolesPermissionsSyncService rolesPermissionsSyncService,
                           PlanAbonnementDomainService planAbonnementDomainService,
                           TypePlanAbonnementDomainService typePlanAbonnementDomainService,
                           AccountDomainService accountDomainService,
                           RoleDomainService roleDomainService,
                           PasswordEncoder passwordEncoder,
                           UtilisateurDomainService utilisateurDomainService,
                           NotificationDomainService notificationDomainService,
                           DemoProductSeeder demoProductSeeder) {
        this.rbacProperties = rbacProperties;
        this.rolesPermissionsSyncService = rolesPermissionsSyncService;
        this.planAbonnementDomainService = planAbonnementDomainService;
        this.typePlanAbonnementDomainService = typePlanAbonnementDomainService;
        this.accountDomainService = accountDomainService;
        this.roleDomainService = roleDomainService;
        this.passwordEncoder = passwordEncoder;
        this.utilisateurDomainService = utilisateurDomainService;
        this.notificationDomainService = notificationDomainService;
        this.demoProductSeeder = demoProductSeeder;
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
        if (rbacProperties.sync()) {
            seedSampleNotifications();
            demoProductSeeder.seed();
        }
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

    /**
     * Seeds sample IN_APP notifications on the ADMIN account so the notification UI can be previewed.
     * Idempotent: only seeds when the admin account has zero notifications.
     */
    private void seedSampleNotifications() {
        Account adminAccount = accountDomainService.findByUsername(ADMIN_USERNAME).orElse(null);
        if (adminAccount == null) return;

        long existing = notificationDomainService.countUnread(adminAccount.getId())
                + notificationDomainService.findByDestinataire(adminAccount.getId(),
                        PageRequest.of(0, 1))
                        .getTotalElements();
        if (existing > 0) return;

        LocalDateTime now = LocalDateTime.now();

        seedNotif(adminAccount, "Contact : Demande de démo",
                "Amadou Diallo <amadou@example.com>\nBonjour, je souhaite une démonstration de votre solution ERP.",
                NotificationStatut.ENVOYEE, now.minusHours(2));

        seedNotif(adminAccount, "Nouveau paiement à valider — SARL Alpha",
                "L'entreprise « SARL Alpha » a soumis un paiement de 15 000 XOF. En attente de validation.",
                NotificationStatut.ENVOYEE, now.minusHours(5));

        seedNotif(adminAccount, "Contact : Problème de connexion",
                "Fatou Ndiaye <fatou@store-client.sn>\nJe n'arrive pas à me connecter à mon espace. Mon username est fndaye.",
                NotificationStatut.ENVOYEE, now.minusDays(1));

        seedNotif(adminAccount, "Nouveau paiement à valider — GIE Soleil",
                "L'entreprise « GIE Soleil » a soumis un paiement de 45 000 XOF. En attente de validation.",
                NotificationStatut.LUE, now.minusDays(2));

        seedNotif(adminAccount, "Contact : Question sur les tarifs",
                "Omar Ba <oba@gmail.com>\nQuels sont vos tarifs pour une PME de 10 employés ?",
                NotificationStatut.LUE, now.minusDays(3));

        log.info("DataInitializer: 5 notifications de démo seedées pour le compte ADMIN");
    }

    private void seedNotif(Account dest, String titre, String message,
                           NotificationStatut statut, LocalDateTime dateEnvoi) {
        Notification n = new Notification();
        n.setDestinataire(dest);
        n.setTitre(titre);
        n.setMessage(message);
        n.setCanal(CanalNotification.IN_APP);
        n.setStatut(statut);
        n.setDateEnvoi(dateEnvoi);
        notificationDomainService.save(n);
    }
}
