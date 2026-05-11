package org.store.abonnement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.PlanAbonnementRepository;

import java.util.UUID;

public interface PlanAbonnementJpaRepository extends JpaRepository<PlanAbonnement, UUID>, PlanAbonnementRepository {
}
