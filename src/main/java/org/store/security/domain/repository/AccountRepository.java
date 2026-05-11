package org.store.security.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.security.domain.model.Account;

import java.util.Optional;

public interface AccountRepository extends BaseRepository<Account> {

    Optional<Account> findByUsername(String username);
}
