package org.store.abonnement.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.TypeAbonnement;

import java.util.UUID;

public interface TypeAbonnementJpaRepository extends JpaRepository<TypeAbonnement, UUID> {
}
