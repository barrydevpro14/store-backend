package org.store.paiement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.repository.MoyenPaiementRepository;

import java.util.UUID;

@Repository
public interface MoyenPaiementJpaRepository extends JpaRepository<MoyenPaiement, UUID>, MoyenPaiementRepository {
}
