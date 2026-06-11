package org.store.vente.application.dto;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClientFilterTest {

    @Test
    void createdStartDateTime_should_return_start_of_day_when_date_provided() {
        LocalDate date = LocalDate.of(2025, 5, 10);
        ClientFilter filter = new ClientFilter(null, null, null, date, null, 0, 10);

        LocalDateTime result = filter.createdStartDateTime();

        assertThat(result).isEqualTo(LocalDateTime.of(2025, 5, 10, 0, 0, 0));
    }

    @Test
    void createdStartDateTime_should_return_null_when_no_start_date() {
        ClientFilter filter = new ClientFilter(null, null, null, null, null, 0, 10);

        assertThat(filter.createdStartDateTime()).isNull();
    }

    @Test
    void createdEndDateTime_should_return_start_of_next_day_when_date_provided() {
        LocalDate date = LocalDate.of(2025, 5, 20);
        ClientFilter filter = new ClientFilter(null, null, null, null, date, 0, 10);

        LocalDateTime result = filter.createdEndDateTime();

        assertThat(result).isEqualTo(LocalDateTime.of(2025, 5, 21, 0, 0, 0));
    }

    @Test
    void createdEndDateTime_should_return_null_when_no_end_date() {
        ClientFilter filter = new ClientFilter(null, null, null, null, null, 0, 10);

        assertThat(filter.createdEndDateTime()).isNull();
    }

    @Test
    void toPageable_should_return_pageable_with_correct_page_and_size() {
        ClientFilter filter = new ClientFilter("Diallo", "Mama", null, null, null, 2, 15);

        Pageable pageable = filter.toPageable();

        assertThat(pageable).isEqualTo(PageRequest.of(2, 15));
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(15);
    }

    @Test
    void toPageable_should_return_first_page_at_page_zero() {
        ClientFilter filter = new ClientFilter(null, null, null, null, null, 0, 10);

        Pageable pageable = filter.toPageable();

        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }
}
