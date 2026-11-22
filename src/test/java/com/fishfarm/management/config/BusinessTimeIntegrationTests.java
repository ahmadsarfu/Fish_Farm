package com.fishfarm.management.config;

import jakarta.validation.Validation;
import jakarta.validation.constraints.PastOrPresent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BusinessTimeIntegrationTests {
    @Autowired Clock clock;
    @Autowired JdbcTemplate jdbc;
    record DatedEntry(@PastOrPresent LocalDate date) {}
    @Test void businessClockUsesIstAndDatabaseSessionUsesUtc() {
        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Kolkata"));
        assertThat(jdbc.queryForObject("SELECT @@session.time_zone",String.class)).isEqualTo("+00:00");
    }
    @Test void validationAcceptsNewIstDayBeforeUtcMidnight() {
        Clock midnight=Clock.fixed(Instant.parse("2026-11-22T18:30:00Z"),clock.getZone());
        var configuration=Validation.byDefaultProvider().configure();
        new BusinessTimeConfig().businessValidationClock(midnight).customize(configuration);
        try(var factory=configuration.buildValidatorFactory()) {
            assertThat(factory.getValidator().validate(new DatedEntry(LocalDate.of(2026,11,23)))).isEmpty();
            assertThat(factory.getValidator().validate(new DatedEntry(LocalDate.of(2026,11,24)))).hasSize(1);
        }
    }
}
