package org.store.users.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.repository.ProprietaireRepository;

import java.util.UUID;

public interface ProprietaireJpaRepository extends JpaRepository<Proprietaire, UUID>, ProprietaireRepository {
}
