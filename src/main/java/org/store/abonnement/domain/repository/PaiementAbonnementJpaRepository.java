package org.store.abonnement.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.PaiementAbonnement;

import java.util.UUID;

public interface PaiementAbonnementJpaRepository extends JpaRepository<PaiementAbonnement, UUID> {
}
