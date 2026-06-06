package org.store.security.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.security.domain.model.Account;
import org.store.security.domain.repository.AccountRepository;

import java.util.UUID;

@Repository
public interface AccountJpaRepository extends JpaRepository<Account, UUID>, AccountRepository {
}
