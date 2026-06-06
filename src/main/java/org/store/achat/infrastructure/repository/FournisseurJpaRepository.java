package org.store.achat.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.achat.domain.model.Fournisseur;
import org.store.achat.domain.repository.FournisseurRepository;

import java.util.UUID;

@Repository
public interface FournisseurJpaRepository extends JpaRepository<Fournisseur, UUID>, FournisseurRepository {
}
