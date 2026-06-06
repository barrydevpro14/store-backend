package org.store.abonnement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.abonnement.domain.model.TypePlanAbonnement;
import org.store.abonnement.domain.repository.TypePlanAbonnementRepository;

import java.util.UUID;

@Repository
public interface TypePlanAbonnementJpaRepository extends JpaRepository<TypePlanAbonnement, UUID>, TypePlanAbonnementRepository {
}
