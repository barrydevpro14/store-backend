package org.store.abonnement.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.Abonnement;

import java.util.UUID;

public interface AbonnementJpaRepository extends JpaRepository<Abonnement, UUID> {
}
