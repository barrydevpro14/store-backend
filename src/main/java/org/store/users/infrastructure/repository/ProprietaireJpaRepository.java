package org.store.users.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.repository.ProprietaireRepository;

import java.util.UUID;

@Repository
public interface ProprietaireJpaRepository extends JpaRepository<Proprietaire, UUID>, ProprietaireRepository {
}
