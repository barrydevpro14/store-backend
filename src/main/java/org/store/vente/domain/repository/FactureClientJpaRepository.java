package org.store.vente.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.vente.domain.model.FactureClient;

import java.util.UUID;

public interface FactureClientJpaRepository extends JpaRepository<FactureClient, UUID> {
}
