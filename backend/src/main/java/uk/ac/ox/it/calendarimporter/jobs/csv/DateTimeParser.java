package uk.ac.ox.it.calendarimporter.jobs.csv;

import com.google.common.primitives.Ints;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the date, time, duration parsing of the CSV fields.
 */
public class DateTimeParser {

    public static final DateTimeFormatter UK_DATE = new DateTimeFormatterBuilder()
            .appendPattern("d/M/")
            .optionalStart()
            .appendPattern("uuuu")
            .optionalEnd()
            .optionalStart()
            .appendValueReduced(ChronoField.YEAR, 2, 2, 1990)
            .optionalEnd()
            .toFormatter();

    static Duration parseDuration(String string) {
        Matcher matcher = Pattern.compile("(?:(?<hours>\\d+):)?(?<minutes>-?\\d+)").matcher(string);
        if (!matcher.matches()) {
            throw new RuntimeException("Duration is not valid");
        }
        String minutes = matcher.group("minutes");
        Duration duration = Duration.ofMinutes(Ints.tryParse(minutes));
        String hours = matcher.group("hours");
        if (hours != null) {
            duration = duration.plusHours(Ints.tryParse(hours));
        }
        if (duration.isNegative()) {
            throw new RuntimeException("Cannot have negative duration: " + string);
        }
        return duration;
    }

    /**
     * Parses dates in the following formats:
     * - yyyy-MM-dd (ISO)
     * - dd/MM/yyyy or d/M/yyyy (UK format)
     * - dd/MM/yy or d/M/yy (UK format, 2-digit year)
     */
    static LocalDate parseDate(String string) {
        List<DateTimeFormatter> patterns = new ArrayList<>();
        // We use ISO for standard imports.
        patterns.add(DateTimeFormatter.ISO_DATE);
        // Users generally prefer using their local date
        patterns.add(UK_DATE);

        for (DateTimeFormatter pattern : patterns) {
            try {
                return pattern.parse(string, LocalDate::from);
            } catch (DateTimeParseException e) {
                // Try another pattern
            }
        }
        throw new RuntimeException("Failed to parse date: " + string +
                ". Please use format DD/MM/YYYY or YYYY-MM-DD");
    }

    static LocalTime parseTime(String string) {
        return parseTime(string, false);
    }

    static LocalTime parseTime(String string, boolean caseSensitive) {
        List<DateTimeFormatter> patterns = new ArrayList<>();
        patterns.add(DateTimeFormatter.ofPattern("hh:mm a"));
        patterns.add(DateTimeFormatter.ofPattern("hh:mm:ss a"));
        patterns.add(DateTimeFormatter.ofPattern("HH:mm"));
        patterns.add(DateTimeFormatter.ofPattern("HH:mm:ss"));
        // Java 8 Requires AM/PM to be in upper case (Java 11 requires it to be lowercase)
        for (DateTimeFormatter pattern : patterns) {
            try {
                if (caseSensitive) {
                    return pattern.parse(string, LocalTime::from);
                } else {
                    try {
                        return pattern.parse(string.toUpperCase(), LocalTime::from);
                    } catch (DateTimeParseException ignore) {
                    }
                    return pattern.parse(string.toLowerCase(), LocalTime::from);
                }
            } catch (DateTimeParseException e) {
                // Try another pattern.
            }
        }
        throw new RuntimeException("Failed to parse time: " + string);
    }
}
