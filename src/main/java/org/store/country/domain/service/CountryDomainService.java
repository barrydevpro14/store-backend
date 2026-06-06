package org.store.country.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.country.domain.model.Country;
import org.store.country.domain.repository.CountryRepository;

import java.util.List;

@Service
public class CountryDomainService extends GlobalService<Country, CountryRepository> {

    public CountryDomainService(CountryRepository repository) {
        super(repository);
    }

    public List<Country> findAllActive() {
        return repository.findByActifTrueOrderByNameAsc();
    }
}
