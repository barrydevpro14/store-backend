package org.store.country.application.dto;

import org.store.country.domain.model.Country;

import java.util.UUID;

public record CountryResponse(UUID id, String name, String countryCode, String currency) {
    public CountryResponse(Country country) {
        this(country.getId(), country.getName(), country.getCountryCode(), country.getCurrency());
    }
}
