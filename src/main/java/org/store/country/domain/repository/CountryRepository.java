package org.store.country.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.country.domain.model.Country;

import java.util.List;

public interface CountryRepository extends BaseRepository<Country> {
    List<Country> findByActifTrueOrderByNameAsc();
}
