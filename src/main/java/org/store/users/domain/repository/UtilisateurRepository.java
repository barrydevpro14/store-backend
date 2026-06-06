package org.store.users.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.users.domain.model.Utilisateur;

import java.util.Optional;
import java.util.UUID;

public interface UtilisateurRepository extends BaseRepository<Utilisateur> {

    Optional<Utilisateur> findByAccountId(UUID accountId);

    boolean existsByEmail(String email);

    boolean existsByTelephone(String telephone);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByTelephoneAndIdNot(String telephone, UUID id);
}
