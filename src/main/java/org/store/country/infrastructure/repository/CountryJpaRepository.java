package org.store.country.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.store.country.domain.model.Country;
import org.store.country.domain.repository.CountryRepository;

import java.util.UUID;

public interface CountryJpaRepository extends JpaRepository<Country, UUID>, CountryRepository {}
