package org.store.vente.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.vente.domain.model.PaiementVente;
import org.store.vente.domain.repository.PaiementVenteRepository;

import java.util.UUID;

public interface PaiementVenteJpaRepository extends JpaRepository<PaiementVente, UUID>, PaiementVenteRepository {
}
