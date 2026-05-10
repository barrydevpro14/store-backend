package org.store.users.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.users.domain.model.Proprietaire;

import java.util.UUID;

public interface ProprietaireJpaRepository extends JpaRepository<Proprietaire, UUID> {
}
