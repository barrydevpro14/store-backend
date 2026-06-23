package org.store.achat.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.repository.CommandeAchatRepository;

import java.util.UUID;

@Repository
public interface CommandeAchatJpaRepository extends JpaRepository<CommandeAchat, UUID>,
        JpaSpecificationExecutor<CommandeAchat>, CommandeAchatRepository {
}
