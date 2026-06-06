package org.store.vente.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.vente.domain.model.LigneCommandeVente;
import org.store.vente.domain.repository.LigneCommandeVenteRepository;

import java.util.UUID;

@Repository
public interface LigneCommandeVenteJpaRepository extends JpaRepository<LigneCommandeVente, UUID>, LigneCommandeVenteRepository {
}
