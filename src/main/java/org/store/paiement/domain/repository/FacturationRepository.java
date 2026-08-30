package org.store.paiement.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.paiement.application.dto.FacturationOptionResponse;
import org.store.paiement.application.dto.FacturationResponse;
import org.store.paiement.domain.model.Facturation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface FacturationRepository extends BaseRepository<Facturation> {

    @Query("""
            SELECT COUNT(facturation) > 0
            FROM Facturation facturation
            WHERE facturation.moyenPaiement.id = :moyenPaiementId
              AND ((:paysId IS NULL AND facturation.pays IS NULL) OR facturation.pays.id = :paysId)
              AND (:excludeId IS NULL OR facturation.id <> :excludeId)
            """)
    boolean existsByMoyenAndPays(@Param("moyenPaiementId") UUID moyenPaiementId,
                                  @Param("paysId") UUID paysId,
                                  @Param("excludeId") UUID excludeId);

    @Query(value = """
            SELECT new org.store.paiement.application.dto.FacturationResponse(facturation)
            FROM Facturation facturation
            WHERE (:moyenPaiementId IS NULL OR facturation.moyenPaiement.id = :moyenPaiementId)
              AND (:paysId IS NULL OR facturation.pays.id = :paysId)
              AND (:actif IS NULL OR facturation.actif = :actif)
              AND (:createdStart IS NULL OR facturation.createdAt >= :createdStart)
              AND (:createdEnd IS NULL OR facturation.createdAt < :createdEnd)
            ORDER BY facturation.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(facturation)
            FROM Facturation facturation
            WHERE (:moyenPaiementId IS NULL OR facturation.moyenPaiement.id = :moyenPaiementId)
              AND (:paysId IS NULL OR facturation.pays.id = :paysId)
              AND (:actif IS NULL OR facturation.actif = :actif)
              AND (:createdStart IS NULL OR facturation.createdAt >= :createdStart)
              AND (:createdEnd IS NULL OR facturation.createdAt < :createdEnd)
            """)
    Page<FacturationResponse> findResponsesByFilter(@Param("moyenPaiementId") UUID moyenPaiementId,
                                                      @Param("paysId") UUID paysId,
                                                      @Param("actif") Boolean actif,
                                                      @Param("createdStart") LocalDateTime createdStart,
                                                      @Param("createdEnd") LocalDateTime createdEnd,
                                                      Pageable pageable);

    /**
     * Returns the active facturation options visible for the given country: country-specific
     * facturations for that country, plus global facturations whose moyenPaiement has no active
     * country-specific override for that same country (country-specific overrides global).
     */
    @Query("""
            SELECT new org.store.paiement.application.dto.FacturationOptionResponse(
                facturation.id, facturation.moyenPaiement.libelle, facturation.numeroFacturation)
            FROM Facturation facturation
            WHERE facturation.actif = true
              AND facturation.moyenPaiement.actif = true
              AND (
                    facturation.pays.id = :countryId
                    OR (
                         facturation.pays IS NULL
                         AND NOT EXISTS (
                             SELECT 1
                             FROM Facturation countryOverride
                             WHERE countryOverride.moyenPaiement = facturation.moyenPaiement
                               AND countryOverride.pays.id = :countryId
                               AND countryOverride.actif = true
                         )
                       )
                  )
            ORDER BY facturation.moyenPaiement.libelle ASC
            """)
    List<FacturationOptionResponse> findSelectOptions(@Param("countryId") UUID countryId);
}
