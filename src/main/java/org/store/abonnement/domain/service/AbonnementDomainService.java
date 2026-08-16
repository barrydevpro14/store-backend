package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.AbonnementRepository;
import org.store.common.service.GlobalService;
import org.store.entreprise.domain.model.Entreprise;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain service for Abonnement rows. Holds both paid subscriptions (ACTIF / EN_ATTENTE / EXPIRE /
 * SUSPENDU) and free-trial windows (statut=TRIAL). The TRIAL row is created at signup by the
 * register flow and surfaces in {@link #findCurrent}.
 */
@Service
public class AbonnementDomainService extends GlobalService<Abonnement, AbonnementRepository> {

    public AbonnementDomainService(AbonnementRepository repository) {
        super(repository);
    }

    /**
     * Free-trial Abonnement created at OWNER signup: dateDebut=today, dateFin=today+trialDays, statut=TRIAL.
     */
    public Abonnement createTrial(Entreprise entreprise, PlanAbonnement planAbonnement, int trialDays) {
        Abonnement abonnement = new Abonnement();
        abonnement.setEntreprise(entreprise);
        abonnement.setPlanAbonnement(planAbonnement);
        abonnement.setDateDebut(LocalDate.now());
        abonnement.setDateFin(LocalDate.now().plusDays(trialDays));
        abonnement.setStatut(AbonnementStatut.TRIAL);
        return save(abonnement);
    }

    /**
     * Paid subscription starts EN_ATTENTE and is activated by {@link #activate} after payment validation.
     */
    public Abonnement createPending(Entreprise entreprise, PlanAbonnement planAbonnement, PeriodiciteAbonnement periodicite) {
        Abonnement abonnement = new Abonnement();
        abonnement.setEntreprise(entreprise);
        abonnement.setPlanAbonnement(planAbonnement);
        abonnement.setPeriodicite(periodicite);
        abonnement.setStatut(AbonnementStatut.EN_ATTENTE);
        return save(abonnement);
    }

    /** Flips the Abonnement to statut=ACTIF and applies the validated paiement's date window. */
    public Abonnement activate(Abonnement abonnement, LocalDate dateDebut, LocalDate dateFin) {
        abonnement.setDateDebut(dateDebut);
        abonnement.setDateFin(dateFin);
        abonnement.setStatut(AbonnementStatut.ACTIF);
        return save(abonnement);
    }

    public Optional<Abonnement> findCurrentActif(UUID entrepriseId) {
        return repository.findFirstByEntrepriseAndStatut(entrepriseId, AbonnementStatut.ACTIF);
    }

    /**
     * Returns the caller's "current" subscription: ACTIF if a paid one exists, otherwise a TRIAL
     * still in its window. Expired trials and EN_ATTENTE rows are ignored.
     * Pageable.ofSize(1) ensures the ORDER BY (ACTIF before TRIAL) is applied at DB level so
     * ACTIF is always returned first, and a data anomaly never causes NonUniqueResultException.
     */
    public Optional<Abonnement> findCurrent(UUID entrepriseId) {
        return repository.findCurrentByEntreprise(entrepriseId, LocalDate.now(), Pageable.ofSize(1))
                         .stream().findFirst();
    }

    /** Expires the TRIAL subscription for the entreprise if one exists (called on first payment activation). */
    public void expireTrialIfAny(UUID entrepriseId) {
        repository.findFirstByEntrepriseAndStatut(entrepriseId, AbonnementStatut.TRIAL)
                  .ifPresent(trial -> {
                      trial.setStatut(AbonnementStatut.EXPIRE);
                      save(trial);
                  });
    }

    public Optional<LocalDate> findLatestActifDateFin(UUID entrepriseId, UUID excludeAbonnementId) {
        return repository.findLatestActifDateFin(entrepriseId, excludeAbonnementId);
    }

    public Page<AbonnementResponse> findResponses(AbonnementFilter filter) {
        return repository.findResponsesByFilter(
                filter.entrepriseId(), filter.statutAsEnum(), filter.planId(),
                filter.startDate(), filter.endDate(),
                filter.toPageable());
    }

    /** Returns the entreprise's EN_ATTENTE Abonnement projected as a response, or empty when none. */
    public Optional<AbonnementResponse> findPendingResponseByEntreprise(UUID entrepriseId) {
        return repository.findPendingResponseByEntreprise(entrepriseId);
    }

    /** Compte le nombre d'abonnements dans un statut donné. */
    public long countByStatut(AbonnementStatut statut) {
        return repository.countByStatut(statut);
    }

    /** Finds active/trial subscriptions expiring on the given date (for daily alert scheduler). */
    public java.util.List<Abonnement> findExpiringOn(java.time.LocalDate date) {
        return repository.findByDateFinAndStatutActifOrTrial(date);
    }

    /** Counts Abonnements created within an optional date range. Both bounds are optional. */
    /** Returns true if the entreprise already has an EN_ATTENTE abonnement awaiting payment. */
    public boolean hasPendingByEntreprise(UUID entrepriseId) {
        return repository.existsByEntrepriseIdAndStatut(entrepriseId, AbonnementStatut.EN_ATTENTE);
    }

    /** Returns true if the entreprise's subscription is currently SUSPENDU (non-payment). */
    public boolean isSuspendu(UUID entrepriseId) {
        return repository.existsByEntrepriseIdAndStatut(entrepriseId, AbonnementStatut.SUSPENDU);
    }

    /** Returns true if the entreprise's subscription is INACTIF (admin-deactivated). */
    public boolean isInactif(UUID entrepriseId) {
        return repository.existsByEntrepriseIdAndStatut(entrepriseId, AbonnementStatut.INACTIF);
    }

    /**
     * Returns the statut of the most recently created SUSPENDU or INACTIF subscription.
     * Prevents false positives when an old SUSPENDU row co-exists with a newer INACTIF row.
     */
    public Optional<AbonnementStatut> findCurrentNonActiveStatut(UUID entrepriseId) {
        return repository.findLatestSuspendedOrInactif(entrepriseId, Pageable.ofSize(1))
                         .stream().findFirst()
                         .map(Abonnement::getStatut);
    }

    public long countByCreatedBetween(String startDate, String endDate) {
        return repository.countByCreatedBetween(startDate, endDate);
    }

    /** Suspends an active subscription due to non-payment (scheduler use). */
    public Abonnement suspend(Abonnement abonnement) {
        abonnement.setStatut(AbonnementStatut.SUSPENDU);
        return save(abonnement);
    }

    /**
     * Admin deactivation: EN_ATTENTE → EXPIRE (request never completed),
     * ACTIF / TRIAL / SUSPENDU → INACTIF (admin-disabled, distinct from non-payment SUSPENDU).
     */
    public Abonnement cancel(Abonnement abonnement) {
        AbonnementStatut next = abonnement.getStatut() == AbonnementStatut.EN_ATTENTE
                ? AbonnementStatut.EXPIRE
                : AbonnementStatut.INACTIF;
        abonnement.setStatut(next);
        return save(abonnement);
    }

    /** Admin reactivation: INACTIF → ACTIF, dateFin preserved (option A). */
    public Abonnement reactivate(Abonnement abonnement) {
        abonnement.setStatut(AbonnementStatut.ACTIF);
        return save(abonnement);
    }

    /** Finds TRIAL subscriptions expiring on any of the given alert dates (today, today+1, today+3, today+5). */
    public List<Abonnement> findExpiringOnDates(List<LocalDate> dates) {
        return repository.findByDateFinInAndStatutActifOrTrial(dates, List.of(AbonnementStatut.TRIAL));
    }

    /** Finds ACTIF subscriptions whose dateFin equals targetDate, for pre-billing 10 days in advance. */
    public List<Abonnement> findAbonnementsToFacture(LocalDate targetDate) {
        return repository.findAbonnementsToFacture(targetDate);
    }
}
