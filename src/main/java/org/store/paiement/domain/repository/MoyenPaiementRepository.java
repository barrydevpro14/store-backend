package org.store.paiement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.dto.DataSelect;
import org.store.common.repository.BaseRepository;
import org.store.paiement.domain.model.MoyenPaiement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MoyenPaiementRepository extends BaseRepository<MoyenPaiement> {

    Optional<MoyenPaiement> findByCode(String code);

    List<MoyenPaiement> findAllByActifTrue();

    @Query(value = """
            SELECT DISTINCT new org.store.common.dto.DataSelect(CAST(moyenPaiement.id AS string), moyenPaiement.libelle)
            FROM MoyenPaiement moyenPaiement
            LEFT JOIN moyenPaiement.pays pays
            WHERE moyenPaiement.actif = true
              AND (:countryId IS NULL OR pays IS NULL OR pays.id = :countryId)
              AND (:searchTerm IS NULL OR :searchTerm = '' OR LOWER(moyenPaiement.libelle) LIKE :searchPattern)
            ORDER BY moyenPaiement.libelle ASC
            """,
           countQuery = """
            SELECT COUNT(DISTINCT moyenPaiement.id)
            FROM MoyenPaiement moyenPaiement
            LEFT JOIN moyenPaiement.pays pays
            WHERE moyenPaiement.actif = true
              AND (:countryId IS NULL OR pays IS NULL OR pays.id = :countryId)
              AND (:searchTerm IS NULL OR :searchTerm = '' OR LOWER(moyenPaiement.libelle) LIKE :searchPattern)
            """)
    Page<DataSelect> findSelectItems(@Param("countryId") UUID countryId,
                                      @Param("searchTerm") String searchTerm,
                                      @Param("searchPattern") String searchPattern,
                                      Pageable pageable);
}
