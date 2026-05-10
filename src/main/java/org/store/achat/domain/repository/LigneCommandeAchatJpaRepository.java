package org.store.achat.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.achat.domain.model.LigneCommandeAchat;

import java.util.UUID;

public interface LigneCommandeAchatJpaRepository extends JpaRepository<LigneCommandeAchat, UUID> {
}
