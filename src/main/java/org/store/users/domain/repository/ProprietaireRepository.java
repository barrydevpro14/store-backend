package org.store.users.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.security.domain.model.Account;
import org.store.users.domain.model.Proprietaire;

import java.util.Optional;
import java.util.UUID;

public interface ProprietaireRepository extends BaseRepository<Proprietaire> {

    @Query("SELECT p.account FROM Proprietaire p WHERE p.entreprise.id = :entrepriseId")
    Optional<Account> findAccountByEntrepriseId(@Param("entrepriseId") UUID entrepriseId);
}
