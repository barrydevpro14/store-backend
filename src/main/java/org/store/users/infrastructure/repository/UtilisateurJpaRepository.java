package org.store.users.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.users.domain.model.Utilisateur;
import org.store.users.domain.repository.UtilisateurRepository;

import java.util.UUID;

public interface UtilisateurJpaRepository extends JpaRepository<Utilisateur, UUID>, UtilisateurRepository {
}
