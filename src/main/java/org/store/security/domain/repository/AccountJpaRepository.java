package org.store.security.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.security.domain.model.Account;

import java.util.UUID;

public interface AccountJpaRepository extends JpaRepository<Account, UUID> {
}
