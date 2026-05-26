package org.store.country.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.country.application.dto.CountryResponse;
import org.store.country.domain.service.CountryDomainService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/countries")
public class CountryController {

    private final CountryDomainService countryDomainService;

    public CountryController(CountryDomainService countryDomainService) {
        this.countryDomainService = countryDomainService;
    }

    @GetMapping
    public ResponseEntity<List<CountryResponse>> listActive() {
        return ResponseEntity.ok(
                countryDomainService.findAllActive().stream()
                        .map(CountryResponse::new)
                        .toList()
        );
    }
}
