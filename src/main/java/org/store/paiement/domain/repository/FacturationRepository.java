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
import java.util.Set;
import java.util.UUID;

public interface FacturationRepository extends BaseRepository<Facturation> {

    /** True when another facturation for this moyen already claims one of the given countries. */
    @Query("""
            SELECT COUNT(other) > 0
            FROM Facturation other
            JOIN other.pays otherPays
            WHERE other.moyenPaiement.id = :moyenPaiementId
              AND (:excludeId IS NULL OR other.id <> :excludeId)
              AND otherPays.id IN :paysIds
            """)
    boolean existsWithOverlappingCountry(@Param("moyenPaiementId") UUID moyenPaiementId,
                                          @Param("paysIds") Set<UUID> paysIds,
                                          @Param("excludeId") UUID excludeId);

    /** True when another global (empty pays) facturation already exists for this moyen. */
    @Query("""
            SELECT COUNT(other) > 0
            FROM Facturation other
            WHERE other.moyenPaiement.id = :moyenPaiementId
              AND other.pays IS EMPTY
              AND (:excludeId IS NULL OR other.id <> :excludeId)
            """)
    boolean existsGlobal(@Param("moyenPaiementId") UUID moyenPaiementId,
                          @Param("excludeId") UUID excludeId);

    /**
     * `createdStart`/`createdEnd` are always non-null (caller resolves them via
     * {@code DateHelper.coalesceStart/coalesceEnd}) so the comparison stays unconditional —
     * binding an actual {@code null} `LocalDateTime` here makes PostgreSQL unable to determine
     * that parameter's data type ("could not determine data type of parameter $n").
     */
    @Query(value = """
            SELECT new org.store.paiement.application.dto.FacturationResponse(facturation)
            FROM Facturation facturation
            WHERE (:moyenPaiementId IS NULL OR facturation.moyenPaiement.id = :moyenPaiementId)
              AND (:paysId IS NULL OR EXISTS (
                    SELECT 1 FROM Facturation matching JOIN matching.pays matchingPays
                    WHERE matching = facturation AND matchingPays.id = :paysId
                  ))
              AND (:numeroFacturationPattern IS NULL OR LOWER(facturation.numeroFacturation) LIKE :numeroFacturationPattern)
              AND (:actif IS NULL OR facturation.actif = :actif)
              AND facturation.createdAt >= :createdStart
              AND facturation.createdAt < :createdEnd
            ORDER BY facturation.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(facturation)
            FROM Facturation facturation
            WHERE (:moyenPaiementId IS NULL OR facturation.moyenPaiement.id = :moyenPaiementId)
              AND (:paysId IS NULL OR EXISTS (
                    SELECT 1 FROM Facturation matching JOIN matching.pays matchingPays
                    WHERE matching = facturation AND matchingPays.id = :paysId
                  ))
              AND (:numeroFacturationPattern IS NULL OR LOWER(facturation.numeroFacturation) LIKE :numeroFacturationPattern)
              AND (:actif IS NULL OR facturation.actif = :actif)
              AND facturation.createdAt >= :createdStart
              AND facturation.createdAt < :createdEnd
            """)
    Page<FacturationResponse> findResponsesByFilter(@Param("moyenPaiementId") UUID moyenPaiementId,
                                                      @Param("paysId") UUID paysId,
                                                      @Param("numeroFacturationPattern") String numeroFacturationPattern,
                                                      @Param("actif") Boolean actif,
                                                      @Param("createdStart") LocalDateTime createdStart,
                                                      @Param("createdEnd") LocalDateTime createdEnd,
                                                      Pageable pageable);

    /** Active facturations whose pays set contains the given country (country-specific match). */
    @Query("""
            SELECT new org.store.paiement.application.dto.FacturationOptionResponse(
                facturation.id, facturation.moyenPaiement.libelle, facturation.numeroFacturation)
            FROM Facturation facturation
            JOIN facturation.pays pays
            WHERE facturation.actif = true
              AND facturation.moyenPaiement.actif = true
              AND pays.id = :countryId
            """)
    List<FacturationOptionResponse> findCountrySpecificOptions(@Param("countryId") UUID countryId);

    /**
     * Active global facturations (empty pays set) whose moyen has no active country-specific
     * override for the given country — a country-specific facturation always wins over a global
     * one for the same moyen.
     */
    @Query("""
            SELECT new org.store.paiement.application.dto.FacturationOptionResponse(
                facturation.id, facturation.moyenPaiement.libelle, facturation.numeroFacturation)
            FROM Facturation facturation
            WHERE facturation.actif = true
              AND facturation.moyenPaiement.actif = true
              AND facturation.pays IS EMPTY
              AND NOT EXISTS (
                  SELECT 1
                  FROM Facturation countryOverride
                  JOIN countryOverride.pays overridePays
                  WHERE countryOverride.moyenPaiement = facturation.moyenPaiement
                    AND overridePays.id = :countryId
                    AND countryOverride.actif = true
              )
            """)
    List<FacturationOptionResponse> findGlobalOptionsWithoutOverrideFor(@Param("countryId") UUID countryId);
}
