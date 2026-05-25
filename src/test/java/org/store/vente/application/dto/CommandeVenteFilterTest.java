package org.store.vente.application.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.vente.domain.enums.CommandeVenteStatut;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommandeVenteFilterTest {

    private static final UUID MAGASIN_ID = UUID.randomUUID();

    @Test
    void statutAsEnum_should_return_correct_enum_value() {
        CommandeVenteFilter filter = buildFilter("VALIDATE");

        CommandeVenteStatut result = filter.statutAsEnum();

        assertThat(result).isEqualTo(CommandeVenteStatut.VALIDATE);
    }

    @Test
    void statutAsEnum_should_return_null_when_statut_is_null() {
        CommandeVenteFilter filter = buildFilter(null);

        CommandeVenteStatut result = filter.statutAsEnum();

        assertThat(result).isNull();
    }

    @Test
    void statutAsEnum_should_return_null_when_statut_is_blank() {
        CommandeVenteFilter filter = buildFilter("");

        CommandeVenteStatut result = filter.statutAsEnum();

        assertThat(result).isNull();
    }

    @Test
    void statutAsEnum_should_throw_when_statut_is_invalid() {
        CommandeVenteFilter filter = buildFilter("INVALID_VALUE");

        assertThatThrownBy(filter::statutAsEnum)
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fromDateTime_should_return_start_of_day_for_valid_date_string() {
        CommandeVenteFilter filter = filterWithDates("2025-06-01", null);

        LocalDateTime result = filter.fromDateTime();

        assertThat(result).isEqualTo(LocalDateTime.of(2025, 6, 1, 0, 0, 0));
    }

    @Test
    void fromDateTime_should_return_null_when_startDate_is_null() {
        CommandeVenteFilter filter = filterWithDates(null, null);

        assertThat(filter.fromDateTime()).isNull();
    }

    @Test
    void fromDateTime_should_return_null_when_startDate_is_blank() {
        CommandeVenteFilter filter = filterWithDates("", null);

        assertThat(filter.fromDateTime()).isNull();
    }

    @Test
    void toDateTime_should_return_end_of_day_for_valid_date_string() {
        CommandeVenteFilter filter = filterWithDates(null, "2025-06-30");

        LocalDateTime result = filter.toDateTime();

        assertThat(result.toLocalDate()).isEqualTo(LocalDate.of(2025, 6, 30));
        assertThat(result.toLocalTime()).isEqualTo(LocalTime.MAX);
    }

    @Test
    void toDateTime_should_return_null_when_endDate_is_null() {
        CommandeVenteFilter filter = filterWithDates(null, null);

        assertThat(filter.toDateTime()).isNull();
    }

    @Test
    void toDateTime_should_return_null_when_endDate_is_blank() {
        CommandeVenteFilter filter = filterWithDates(null, "");

        assertThat(filter.toDateTime()).isNull();
    }

    @Test
    void toPageable_should_return_pageable_with_correct_page_and_size() {
        CommandeVenteFilter filter = new CommandeVenteFilter(
                MAGASIN_ID, null, null, null, null,
                null, null, null, null, null, null, 3, 25);

        Pageable pageable = filter.toPageable();

        assertThat(pageable).isEqualTo(PageRequest.of(3, 25));
        assertThat(pageable.getPageNumber()).isEqualTo(3);
        assertThat(pageable.getPageSize()).isEqualTo(25);
    }

    @Test
    void toPageable_should_return_first_page_at_page_zero() {
        CommandeVenteFilter filter = new CommandeVenteFilter(
                MAGASIN_ID, null, null, null, null,
                null, null, null, null, null, null, 0, 10);

        Pageable pageable = filter.toPageable();

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }

    private static CommandeVenteFilter buildFilter(String statut) {
        return new CommandeVenteFilter(
                MAGASIN_ID, null, null, statut, null,
                null, null, null, null, null, null, 0, 10);
    }

    private static CommandeVenteFilter filterWithDates(String startDate, String endDate) {
        return new CommandeVenteFilter(
                MAGASIN_ID, null, null, null, null,
                BigDecimal.ZERO, new BigDecimal("99999"), startDate, endDate, null, null, 0, 10);
    }
}
