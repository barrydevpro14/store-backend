package org.store.abonnement.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.abonnement.domain.model.Revenu;
import org.store.abonnement.domain.repository.RevenuRepository;

import java.util.UUID;

public interface RevenuJpaRepository extends JpaRepository<Revenu, UUID>, RevenuRepository {
}
