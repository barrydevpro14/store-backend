package org.store.produit.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.produit.application.dto.UniteMesureResponse;
import org.store.produit.domain.model.UniteMesure;

import java.util.List;
import java.util.Optional;

public interface UniteMesureRepository extends BaseRepository<UniteMesure> {

    @Query(value = """
            SELECT new org.store.produit.application.dto.UniteMesureResponse(u)
            FROM UniteMesure u
            WHERE (:libelle IS NULL OR :libelle = '' OR LOWER(u.libelle) LIKE :libellePattern)
              AND (:code    IS NULL OR :code    = '' OR LOWER(u.code)    LIKE :codePattern)
            ORDER BY u.libelle ASC
            """,
           countQuery = """
            SELECT COUNT(u)
            FROM UniteMesure u
            WHERE (:libelle IS NULL OR :libelle = '' OR LOWER(u.libelle) LIKE :libellePattern)
              AND (:code    IS NULL OR :code    = '' OR LOWER(u.code)    LIKE :codePattern)
            """)
    Page<UniteMesureResponse> findResponsesByFilter(
            @Param("libelle") String libelle,
            @Param("libellePattern") String libellePattern,
            @Param("code") String code,
            @Param("codePattern") String codePattern,
            Pageable pageable);

    @Query("SELECT u FROM UniteMesure u ORDER BY u.libelle ASC")
    List<UniteMesure> findAllOrdered();

    Optional<UniteMesure> findByCode(String code);

    @Query("SELECT u FROM UniteMesure u WHERE LOWER(u.code) = LOWER(:value) OR LOWER(u.symbole) = LOWER(:value)")
    Optional<UniteMesure> findByCodeOrSymbole(@Param("value") String value);

    @Query("SELECT COUNT(u) > 0 FROM UniteMesure u WHERE LOWER(u.code) = LOWER(:code)")
    boolean existsByCode(@Param("code") String code);
}
