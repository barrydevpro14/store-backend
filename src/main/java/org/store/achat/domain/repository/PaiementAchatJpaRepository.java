package org.store.achat.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.achat.domain.model.PaiementAchat;

import java.util.UUID;

public interface PaiementAchatJpaRepository extends JpaRepository<PaiementAchat, UUID> {
}
