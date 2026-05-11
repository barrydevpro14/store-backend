package org.store.notification.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.notification.domain.model.Echeance;
import org.store.notification.domain.repository.EcheanceRepository;

import java.util.UUID;

public interface EcheanceJpaRepository extends JpaRepository<Echeance, UUID>, EcheanceRepository {
}
