package org.store.achat.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.achat.domain.model.CommandeAchat;

import java.util.UUID;

public interface CommandeAchatJpaRepository extends JpaRepository<CommandeAchat, UUID> {
}
