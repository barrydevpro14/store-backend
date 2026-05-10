package org.store.vente.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.vente.domain.model.LigneCommandeVente;

import java.util.UUID;

public interface LigneCommandeVenteJpaRepository extends JpaRepository<LigneCommandeVente, UUID> {
}
