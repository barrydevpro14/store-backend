package org.store.users.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.users.domain.model.Employe;
import org.store.users.domain.repository.EmployeRepository;

import java.util.UUID;

public interface EmployeJpaRepository extends JpaRepository<Employe, UUID>, EmployeRepository {
}
