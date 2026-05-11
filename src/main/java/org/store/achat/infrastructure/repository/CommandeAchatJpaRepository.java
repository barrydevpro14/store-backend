package org.store.achat.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.repository.CommandeAchatRepository;

import java.util.UUID;

public interface CommandeAchatJpaRepository extends JpaRepository<CommandeAchat, UUID>, CommandeAchatRepository {
}
