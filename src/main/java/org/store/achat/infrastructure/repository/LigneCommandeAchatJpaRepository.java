package org.store.achat.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.achat.domain.model.LigneCommandeAchat;
import org.store.achat.domain.repository.LigneCommandeAchatRepository;

import java.util.UUID;

@Repository
public interface LigneCommandeAchatJpaRepository extends JpaRepository<LigneCommandeAchat, UUID>, LigneCommandeAchatRepository {
}
