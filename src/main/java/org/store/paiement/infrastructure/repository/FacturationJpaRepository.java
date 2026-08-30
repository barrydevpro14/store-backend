package org.store.paiement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.paiement.domain.model.Facturation;
import org.store.paiement.domain.repository.FacturationRepository;

import java.util.UUID;

@Repository
public interface FacturationJpaRepository extends JpaRepository<Facturation, UUID>, FacturationRepository {
}
