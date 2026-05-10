package org.store.achat.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.achat.domain.model.Fournisseur;

import java.util.UUID;

public interface FournisseurJpaRepository extends JpaRepository<Fournisseur, UUID> {
}
