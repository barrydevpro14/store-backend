package org.store.abonnement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.TypePlanAbonnement;
import org.store.abonnement.domain.repository.AbonnementRepository;
import org.store.common.service.GlobalService;
import org.store.entreprise.domain.model.Entreprise;

import java.time.LocalDate;
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
     * Free-trial Abonnement created at OWNER signup. {@code dateDebut} is today, {@code dateFin}
     * is today + {@code trialDays}, {@code actif=true}, {@code statut=TRIAL}. The {@code type} is
     * the first {@link TypePlanAbonnement} attached to the trial plan.
     */
    public Abonnement createTrial(Entreprise entreprise, TypePlanAbonnement trialType, int trialDays) {
        Abonnement abonnement = new Abonnement();
        abonnement.setEntreprise(entreprise);
        abonnement.setTypePlanAbonnement(trialType);
        abonnement.setDateDebut(LocalDate.now());
        abonnement.setDateFin(LocalDate.now().plusDays(trialDays));
        abonnement.setActif(true);
        abonnement.setRenouvellementAuto(false);
        abonnement.setStatut(AbonnementStatut.TRIAL);
        return save(abonnement);
    }

    /**
     * Paid subscription: the type is mandatory; the plan is implicit via {@code type.plan}. The Abonnement
     * starts EN_ATTENTE and is activated by {@link #activate} after payment validation.
     */
    public Abonnement createPending(Entreprise entreprise, TypePlanAbonnement type) {
        Abonnement abonnement = new Abonnement();
        abonnement.setEntreprise(entreprise);
        abonnement.setTypePlanAbonnement(type);
        abonnement.setActif(false);
        abonnement.setRenouvellementAuto(false);
        abonnement.setStatut(AbonnementStatut.EN_ATTENTE);
        return save(abonnement);
    }

    public Abonnement setRenouvellementAuto(Abonnement abonnement, boolean enabled) {
        abonnement.setRenouvellementAuto(enabled);
        return save(abonnement);
    }

    public Abonnement activate(Abonnement abonnement, LocalDate dateDebut, LocalDate dateFin) {
        abonnement.setDateDebut(dateDebut);
        abonnement.setDateFin(dateFin);
        abonnement.setActif(true);
        abonnement.setStatut(AbonnementStatut.ACTIF);
        return save(abonnement);
    }

    public Optional<Abonnement> findCurrentActif(UUID entrepriseId) {
        return repository.findFirstByEntrepriseAndStatut(entrepriseId, AbonnementStatut.ACTIF);
    }

    /**
     * Returns the caller's "current" subscription: ACTIF if a paid one exists, otherwise a TRIAL
     * still in its window. Expired trials and EN_ATTENTE rows are ignored.
     */
    public Optional<Abonnement> findCurrent(UUID entrepriseId) {
        return repository.findCurrentByEntreprise(entrepriseId, LocalDate.now());
    }

    public Optional<LocalDate> findLatestActifDateFin(UUID entrepriseId, UUID excludeAbonnementId) {
        return repository.findLatestActifDateFin(entrepriseId, excludeAbonnementId);
    }

    public Page<AbonnementResponse> findResponses(AbonnementFilter filter) {
        return repository.findResponsesByFilter(filter, filter.toPageable());
    }
}
