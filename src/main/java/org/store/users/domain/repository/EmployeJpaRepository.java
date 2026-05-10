package org.store.users.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.users.domain.model.Employe;

import java.util.UUID;

public interface EmployeJpaRepository extends JpaRepository<Employe, UUID> {
}
