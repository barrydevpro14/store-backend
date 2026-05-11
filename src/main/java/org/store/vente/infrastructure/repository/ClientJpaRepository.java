package org.store.vente.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.vente.domain.model.Client;
import org.store.vente.domain.repository.ClientRepository;

import java.util.UUID;

public interface ClientJpaRepository extends JpaRepository<Client, UUID>, ClientRepository {
}
