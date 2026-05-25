package org.store.security.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.security.domain.model.Account;

import java.util.Optional;

public interface AccountRepository extends BaseRepository<Account> {

    Optional<Account> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("SELECT account FROM Account account WHERE account.role.libelle = :roleLibelle ORDER BY account.createdAt DESC")
    Page<Account> findAllByRoleLibelle(@Param("roleLibelle") String roleLibelle, Pageable pageable);
}
