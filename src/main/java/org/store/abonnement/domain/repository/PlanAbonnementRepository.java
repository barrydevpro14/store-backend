package org.store.abonnement.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.abonnement.domain.model.PlanAbonnement;

import java.util.Optional;

public interface PlanAbonnementRepository extends BaseRepository<PlanAbonnement> {

    Optional<PlanAbonnement> findFirstByTrialTrueAndActifTrue();
}
