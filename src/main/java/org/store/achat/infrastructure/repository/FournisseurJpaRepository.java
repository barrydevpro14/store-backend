package org.store.achat.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.achat.domain.model.Fournisseur;
import org.store.achat.domain.repository.FournisseurRepository;

import java.util.UUID;

public interface FournisseurJpaRepository extends JpaRepository<Fournisseur, UUID>, FournisseurRepository {
}
