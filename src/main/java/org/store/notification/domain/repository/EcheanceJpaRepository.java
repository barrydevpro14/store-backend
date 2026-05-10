package org.store.notification.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.notification.domain.model.Echeance;

import java.util.UUID;

public interface EcheanceJpaRepository extends JpaRepository<Echeance, UUID> {
}
