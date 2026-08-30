package org.store.country.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.store.common.repository.BaseRepository;
import org.store.country.domain.model.Country;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CountryRepository extends BaseRepository<Country> {
    List<Country> findByActifTrueOrderByNameAsc();

    @Query("""
            SELECT country
            FROM Country country
            WHERE country.id IN :countryIds
            """)
    List<Country> findAllByIdIn(@Param("countryIds") Set<UUID> countryIds);
}
