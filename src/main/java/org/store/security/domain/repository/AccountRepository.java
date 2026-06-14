package org.store.security.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.security.domain.model.Account;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends BaseRepository<Account> {

    Optional<Account> findByUsername(String username);

    @Query("SELECT a FROM Account a LEFT JOIN a.user u WHERE a.username = :identifier OR u.email = :identifier")
    Optional<Account> findByUsernameOrEmail(@Param("identifier") String identifier);

    boolean existsByUsername(String username);

    @Query(value = """
            SELECT account FROM Account account
            LEFT JOIN FETCH account.user
            WHERE account.role.libelle = :roleLibelle
            ORDER BY account.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(account) FROM Account account
            WHERE account.role.libelle = :roleLibelle
            """)
    Page<Account> findAllByRoleLibelle(@Param("roleLibelle") String roleLibelle, Pageable pageable);

    @Query("SELECT account FROM Account account WHERE account.role.libelle = :roleLibelle")
    List<Account> findAllByRoleLibelle(@Param("roleLibelle") String roleLibelle);
}
