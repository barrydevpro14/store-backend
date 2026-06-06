package org.store.vente.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.repository.CommandeVenteRepository;

import java.util.UUID;

@Repository
public interface CommandeVenteJpaRepository extends JpaRepository<CommandeVente, UUID>, CommandeVenteRepository {
}
