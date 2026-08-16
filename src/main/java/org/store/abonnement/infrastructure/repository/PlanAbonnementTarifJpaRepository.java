package org.store.abonnement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.abonnement.domain.model.PlanAbonnementTarif;
import org.store.abonnement.domain.repository.PlanAbonnementTarifRepository;

import java.util.UUID;

@Repository
public interface PlanAbonnementTarifJpaRepository extends JpaRepository<PlanAbonnementTarif, UUID>, PlanAbonnementTarifRepository {
}
