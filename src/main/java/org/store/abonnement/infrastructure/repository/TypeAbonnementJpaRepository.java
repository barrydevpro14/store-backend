package org.store.abonnement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.abonnement.domain.model.TypeAbonnement;
import org.store.abonnement.domain.repository.TypeAbonnementRepository;

import java.util.UUID;

@Repository
public interface TypeAbonnementJpaRepository extends JpaRepository<TypeAbonnement, UUID>, TypeAbonnementRepository {
}
