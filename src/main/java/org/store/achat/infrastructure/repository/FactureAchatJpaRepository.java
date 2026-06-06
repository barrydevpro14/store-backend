package org.store.achat.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.achat.domain.model.FactureAchat;
import org.store.achat.domain.repository.FactureAchatRepository;

import java.util.UUID;

@Repository
public interface FactureAchatJpaRepository extends JpaRepository<FactureAchat, UUID>, FactureAchatRepository {
}
