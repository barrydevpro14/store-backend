package org.store.abonnement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.repository.AbonnementRepository;

import java.util.UUID;

@Repository
public interface AbonnementJpaRepository extends JpaRepository<Abonnement, UUID>, AbonnementRepository {
}
