package org.store.abonnement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.PreuvePaiement;
import org.store.abonnement.domain.repository.PreuvePaiementRepository;

import java.util.UUID;

public interface PreuvePaiementJpaRepository extends JpaRepository<PreuvePaiement, UUID>, PreuvePaiementRepository {
}
