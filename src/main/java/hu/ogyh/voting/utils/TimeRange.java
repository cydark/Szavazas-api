package hu.ogyh.voting.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Félig nyitott időtartomány: {@code from <= t < to}. A felhasználó budapesti naptári napokban adja meg
 * az időszakot (mindkét nap beleszámít), belül UTC időpontokkal dolgozunk.
 */
public record TimeRange(Instant from, Instant to) {

    /** A „nap” budapesti naptári nap (2.3). */
    public static final ZoneId ZONE = ZoneId.of("Europe/Budapest");

    public TimeRange {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to: " + from + " / " + to);
        }
    }

    public static TimeRange ofDay(LocalDate day) {
        return ofDays(day, day);
    }

    /** Mindkét nap beleszámít; a nap hossza az óraátállítás miatt 23 vagy 25 óra is lehet. */
    public static TimeRange ofDays(LocalDate firstDay, LocalDate lastDay) {
        return new TimeRange(
                firstDay.atStartOfDay(ZONE).toInstant(),
                lastDay.plusDays(1).atStartOfDay(ZONE).toInstant());
    }
}
