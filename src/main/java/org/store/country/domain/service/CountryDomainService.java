package org.store.country.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.exceptions.EntityException;
import org.store.common.service.GlobalService;
import org.store.country.domain.model.Country;
import org.store.country.domain.repository.CountryRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CountryDomainService extends GlobalService<Country, CountryRepository> {

    public CountryDomainService(CountryRepository repository) {
        super(repository);
    }

    public List<Country> findAllActive() {
        return repository.findByActifTrueOrderByNameAsc();
    }

    public List<Country> findAllByIds(Set<UUID> countryIds) {
        if (countryIds == null || countryIds.isEmpty()) {
            return List.of();
        }

        List<Country> foundCountries = repository.findAllByIdIn(countryIds);
        if (foundCountries.size() != countryIds.size()) {
            throw new EntityException("entity.notFound", countryIds);
        }

        return foundCountries;
    }
}
