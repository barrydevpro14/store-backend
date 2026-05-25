package org.store.common.tools;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DateHelperTest {

    @Test
    void coalesceStart_should_return_value_when_non_null() {
        LocalDateTime value = LocalDateTime.of(2025, 3, 15, 0, 0);
        assertThat(DateHelper.coalesceStart(value)).isSameAs(value);
    }

    @Test
    void coalesceStart_should_return_sentinel_when_null() {
        assertThat(DateHelper.coalesceStart(null)).isEqualTo(DateHelper.SENTINEL_START);
    }

    @Test
    void coalesceEnd_should_return_value_when_non_null() {
        LocalDateTime value = LocalDateTime.of(2025, 12, 31, 23, 59);
        assertThat(DateHelper.coalesceEnd(value)).isSameAs(value);
    }

    @Test
    void coalesceEnd_should_return_sentinel_when_null() {
        assertThat(DateHelper.coalesceEnd(null)).isEqualTo(DateHelper.SENTINEL_END);
    }

    @Test
    void sentinel_start_is_year_2000() {
        assertThat(DateHelper.SENTINEL_START.getYear()).isEqualTo(2000);
        assertThat(DateHelper.SENTINEL_START.getMonthValue()).isEqualTo(1);
        assertThat(DateHelper.SENTINEL_START.getDayOfMonth()).isEqualTo(1);
    }

    @Test
    void sentinel_end_is_year_2099() {
        assertThat(DateHelper.SENTINEL_END.getYear()).isEqualTo(2099);
        assertThat(DateHelper.SENTINEL_END.getMonthValue()).isEqualTo(12);
        assertThat(DateHelper.SENTINEL_END.getDayOfMonth()).isEqualTo(31);
    }

    @Test
    void parseStartOfDay_should_return_midnight_for_valid_date() {
        LocalDateTime result = DateHelper.parseStartOfDay("2025-06-01");
        assertThat(result).isEqualTo(LocalDateTime.of(2025, 6, 1, 0, 0, 0));
    }

    @Test
    void parseStartOfDay_should_return_null_for_blank() {
        assertThat(DateHelper.parseStartOfDay(null)).isNull();
        assertThat(DateHelper.parseStartOfDay("")).isNull();
        assertThat(DateHelper.parseStartOfDay("   ")).isNull();
    }

    @Test
    void parseEndOfDay_should_return_end_of_day_for_valid_date() {
        LocalDateTime result = DateHelper.parseEndOfDay("2025-06-01");
        assertThat(result.toLocalDate()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(result.getHour()).isEqualTo(23);
        assertThat(result.getMinute()).isEqualTo(59);
    }

    @Test
    void parseEndOfDay_should_return_null_for_blank() {
        assertThat(DateHelper.parseEndOfDay(null)).isNull();
        assertThat(DateHelper.parseEndOfDay("")).isNull();
    }
}
