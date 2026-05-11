package org.store.abonnement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.repository.PaiementAbonnementRepository;

import java.util.UUID;

public interface PaiementAbonnementJpaRepository extends JpaRepository<PaiementAbonnement, UUID>, PaiementAbonnementRepository {
}
