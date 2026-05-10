package org.store.vente.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.vente.domain.model.Client;

import java.util.UUID;

public interface ClientJpaRepository extends JpaRepository<Client, UUID> {
}
