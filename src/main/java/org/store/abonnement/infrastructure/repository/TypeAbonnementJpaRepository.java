package org.store.abonnement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.TypeAbonnement;
import org.store.abonnement.domain.repository.TypeAbonnementRepository;

import java.util.UUID;

public interface TypeAbonnementJpaRepository extends JpaRepository<TypeAbonnement, UUID>, TypeAbonnementRepository {
}
