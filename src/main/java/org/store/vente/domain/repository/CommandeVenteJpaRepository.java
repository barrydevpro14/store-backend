package org.store.vente.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.vente.domain.model.CommandeVente;

import java.util.UUID;

public interface CommandeVenteJpaRepository extends JpaRepository<CommandeVente, UUID> {
}
