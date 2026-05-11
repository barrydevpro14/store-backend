package org.store.achat.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.achat.domain.model.PaiementAchat;
import org.store.achat.domain.repository.PaiementAchatRepository;

import java.util.UUID;

public interface PaiementAchatJpaRepository extends JpaRepository<PaiementAchat, UUID>, PaiementAchatRepository {
}
