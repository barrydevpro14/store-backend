package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.domain.enums.AbonnementStatut;
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
    public Abonnement createPending(Entreprise entreprise, PlanAbonnement planAbonnement) {
        Abonnement abonnement = new Abonnement();
        abonnement.setEntreprise(entreprise);
        abonnement.setPlanAbonnement(planAbonnement);
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
     * still in its window. Expired trials and EN_ATTENTE rows are ignored. Single-row return is
     * guaranteed by the partial unique index {@code abonnement_one_actif_per_entreprise} (V14) +
     * {@link #activate} which deactivates any sibling actif=true row first.
     */
    public Optional<Abonnement> findCurrent(UUID entrepriseId) {
        return repository.findCurrentByEntreprise(entrepriseId, LocalDate.now());
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

    public long countByCreatedBetween(String startDate, String endDate) {
        return repository.countByCreatedBetween(startDate, endDate);
    }

    /** Suspends an active subscription due to non-payment (scheduler use). */
    public Abonnement suspend(Abonnement abonnement) {
        abonnement.setStatut(AbonnementStatut.SUSPENDU);
        return save(abonnement);
    }

    /**
     * Annule un abonnement : EN_ATTENTE → EXPIRE (demande non aboutie),
     * ACTIF / TRIAL / SUSPENDU → SUSPENDU (résiliation admin).
     */
    public Abonnement cancel(Abonnement abonnement) {
        AbonnementStatut next = abonnement.getStatut() == AbonnementStatut.EN_ATTENTE
                ? AbonnementStatut.EXPIRE
                : AbonnementStatut.SUSPENDU;
        abonnement.setStatut(next);
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
