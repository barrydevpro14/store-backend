package org.store.abonnement.application.service;

import org.springframework.data.domain.Page;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.application.dto.CurrentAbonnementResponse;
import org.store.abonnement.application.dto.SubscribeRequest;
import org.store.abonnement.application.dto.SubscribeResponse;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.entreprise.domain.model.Entreprise;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IAbonnementService {

    /** Owner-facing subscribe: creates an EN_ATTENTE Abonnement for the caller's entreprise. */
    SubscribeResponse subscribe(SubscribeRequest subscribeRequest);

    /**
     * Creates the TRIAL Abonnement attached to the entreprise at OWNER signup. Looks up the active
     * trial plan and builds the row with {@code statut=TRIAL}, dateDebut today, dateFin today + trial-days.
     */
    Abonnement createTrialForSignup(Entreprise entreprise);

    /** Internal lookup by id. */
    Abonnement findById(UUID id);

    /** OWNER requests plan change for next cycle. */
    AbonnementResponse changerPlan(UUID planId);

    /** ADMIN listing — all Abonnements filtered by entreprise / statut / plan. No scoping. */
    Page<AbonnementResponse> findAll(AbonnementFilter filter);

    /** OWNER history paginated — auto-scoped to the caller's entreprise. */
    Page<AbonnementResponse> findMyHistory(AbonnementFilter filter);

    /**
     * Returns the caller's "current" Abonnement view (ACTIF or still-running TRIAL). Throws
     * {@code EntityException("abonnement.noActive")} when neither is present.
     */
    CurrentAbonnementResponse findMyCurrent();

    /**
     * Returns the caller's EN_ATTENTE Abonnement (created by subscribe, awaiting first paiement)
     * or empty when none. Consumed by the frontend to decide whether to prompt for payment
     * submission or hide the subscribe catalog.
     */
    java.util.Optional<AbonnementResponse> findMyPending();

    /** Throws {@code BadArgumentException("plan.notSubscribable")} if the plan is inactive, hidden or marked trial. */
    void ensurePlanSubscribable(PlanAbonnement plan);

    /** Throws {@code ForbiddenException("abonnement.notOwned")} if the Abonnement is not owned by the caller. */
    Abonnement ensureBelongsToCurrentEntreprise(Abonnement abonnement);

    /** Marks the entreprise as having consumed its free trial. Idempotent. */
    void consumeTrialIfAny(Entreprise entreprise);

    /** Builds the current-view payload from an Abonnement (works for both ACTIF and TRIAL). */
    CurrentAbonnementResponse buildCurrent(Abonnement abonnement);

    /**
     * Returns {@code true} when the entreprise has an ACTIF Abonnement OR a still-running TRIAL.
     * Used as the login subscription gate.
     */
    boolean hasActiveSubscription(UUID entrepriseId);

    /** Returns {@code true} when the entreprise's subscription is SUSPENDU (non-payment suspension). */
    boolean isSuspendedByEntreprise(UUID entrepriseId);

    /** Returns {@code true} when the entreprise's subscription is INACTIF (admin-deactivated). */
    boolean isInactifByEntreprise(UUID entrepriseId);

    /**
     * Returns the statut of the most recently created SUSPENDU or INACTIF subscription for the
     * enterprise. Used by the auth scope logic to avoid false positives from stale historical rows.
     */
    java.util.Optional<org.store.abonnement.domain.enums.AbonnementStatut> findCurrentNonActiveStatut(UUID entrepriseId);

    /** ADMIN count — number of Abonnements created within the given date range (both bounds optional). */
    long countByCreatedDateRange(String startDate, String endDate);

    /**
     * ADMIN — deactivates a subscription: EN_ATTENTE → EXPIRE, ACTIF/TRIAL/SUSPENDU → INACTIF.
     */
    AbonnementResponse cancelByAdmin(UUID abonnementId);

    /**
     * ADMIN — reactivates an INACTIF subscription: INACTIF → ACTIF, dateFin preserved.
     */
    AbonnementResponse reactivateByAdmin(UUID abonnementId);

    List<Abonnement> findExpiringOnDates(List<LocalDate> dates);

    /** Finds active/trial subscriptions whose dateFin equals targetDate (billing scheduler use). */
    List<Abonnement> findAbonnementsToFacture(LocalDate targetDate);

    /** Marks the abonnement SUSPENDU (non-payment suspension scheduler use). */
    Abonnement suspend(Abonnement abonnement);
}
