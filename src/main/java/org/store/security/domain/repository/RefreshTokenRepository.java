package org.store.security.domain.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.security.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends BaseRepository<RefreshToken> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser_Id(UUID userId);

    @Modifying
    @Query(value = """
        DELETE FROM refresh_token
        WHERE user_id IN (
            SELECT p.id FROM proprietaire p
            JOIN entreprise e ON e.proprietaire_id = p.id
            WHERE e.id = :entrepriseId
            UNION
            SELECT emp.id FROM employees emp
            JOIN magasin m ON m.id = emp.magasin_id
            WHERE m.entreprise_id = :entrepriseId
        )
        """, nativeQuery = true)
    void revokeAllByEntrepriseId(@Param("entrepriseId") UUID entrepriseId);
}
