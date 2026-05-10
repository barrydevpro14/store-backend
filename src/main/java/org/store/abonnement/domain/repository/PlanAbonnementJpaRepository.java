package org.store.abonnement.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.PlanAbonnement;

import java.util.UUID;

public interface PlanAbonnementJpaRepository extends JpaRepository<PlanAbonnement, UUID> {
}
