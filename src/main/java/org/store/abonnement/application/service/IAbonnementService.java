package org.store.abonnement.application.service;

import org.springframework.data.domain.Page;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.application.dto.CurrentAbonnementResponse;
import org.store.abonnement.application.dto.RenouvellementAutoRequest;
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
     * Creates the TRIAL Abonnement attached to the entreprise at OWNER signup. Looks up the first
     * {@code TypePlanAbonnement} of the active trial plan and builds the row with
     * {@code statut=TRIAL}, {@code actif=true}, dateDebut today, dateFin today + trial-days.
     */
    Abonnement createTrialForSignup(Entreprise entreprise);

    /** Internal lookup by id. */
    Abonnement findById(UUID id);

    /** Toggles {@code renouvellementAuto} on an Abonnement owned by the caller's entreprise. */
    AbonnementResponse updateRenouvellementAuto(UUID abonnementId, RenouvellementAutoRequest renouvellementAutoRequest);

    /** ADMIN listing — all Abonnements filtered by entreprise / statut / plan. No scoping. */
    Page<AbonnementResponse> findAll(AbonnementFilter filter);

    /** OWNER history paginated — auto-scoped to the caller's entreprise. */
    Page<AbonnementResponse> findMyHistory(AbonnementFilter filter);

    /**
     * Returns the caller's "current" Abonnement view (ACTIF or still-running TRIAL). Throws
     * {@code EntityException("abonnement.noActive")} when neither is present.
     */
    CurrentAbonnementResponse findMyCurrent();

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

    /** ADMIN count — number of Abonnements created within the given date range (both bounds optional). */
    long countByCreatedDateRange(String startDate, String endDate);

    /**
     * ADMIN — annule un abonnement : EN_ATTENTE → EXPIRE, ACTIF/TRIAL → SUSPENDU.
     * Retourne l'abonnement mis à jour.
     */
    AbonnementResponse cancelByAdmin(UUID abonnementId);

    List<Abonnement> findExpiringOnDates(List<LocalDate> dates);
}
