package org.store.achat.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.achat.domain.model.FactureAchat;
import org.store.achat.domain.repository.FactureAchatRepository;

import java.util.UUID;

public interface FactureAchatJpaRepository extends JpaRepository<FactureAchat, UUID>, FactureAchatRepository {
}
