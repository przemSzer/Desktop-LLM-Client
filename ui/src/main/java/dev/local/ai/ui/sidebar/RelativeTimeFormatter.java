package dev.local.ai.ui.sidebar;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

final class RelativeTimeFormatter {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter SHORT_DATE =
            DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);

    private RelativeTimeFormatter() {
    }

    static String format(Instant instant, Clock clock) {
        ZoneId zone = clock.getZone();
        LocalDate today = LocalDate.now(clock);
        LocalDate date = LocalDate.ofInstant(instant, zone);
        if (date.equals(today)) {
            return TIME.format(instant.atZone(zone));
        }
        if (date.equals(today.minusDays(1))) {
            return "Yesterday";
        }
        if (date.isAfter(today.minusDays(7))) {
            return DateTimeFormatter.ofPattern("EEE").format(instant.atZone(zone));
        }
        return SHORT_DATE.format(instant.atZone(zone));
    }
}
