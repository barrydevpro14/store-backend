package org.store.users.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.users.domain.model.Utilisateur;

import java.util.UUID;

public interface UtilisateurJpaRepository extends JpaRepository<Utilisateur, UUID> {
}
