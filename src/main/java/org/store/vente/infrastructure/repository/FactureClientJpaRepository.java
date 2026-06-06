package org.store.vente.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.repository.FactureClientRepository;

import java.util.UUID;

@Repository
public interface FactureClientJpaRepository extends JpaRepository<FactureClient, UUID>, FactureClientRepository {
}
