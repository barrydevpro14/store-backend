package org.store.achat.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.achat.domain.model.FactureAchat;

import java.util.UUID;

public interface FactureAchatJpaRepository extends JpaRepository<FactureAchat, UUID> {
}
