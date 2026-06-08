package org.store.notification.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.notification.domain.model.Alerte;
import org.store.notification.domain.repository.AlerteRepository;

import java.util.UUID;

@Repository
public interface AlerteJpaRepository extends JpaRepository<Alerte, UUID>, AlerteRepository {}
