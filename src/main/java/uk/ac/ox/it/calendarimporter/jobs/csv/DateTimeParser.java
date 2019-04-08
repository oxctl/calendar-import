package uk.ac.ox.it.calendarimporter.jobs.csv;

import com.google.common.primitives.Ints;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Handles the date, time, duration parsing of the CSV fields. */
public class DateTimeParser {

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

  static LocalDate parseDate(String string) {
    // TODO Locale handling, at the moment it's all in the default locale
    List<DateTimeFormatter> patterns = new ArrayList<>();
    patterns.add(
        new DateTimeFormatterBuilder()
                .appendPattern("d/M/")
                .optionalStart()
                .appendPattern("uuuu")
                .optionalEnd()
                .optionalStart()
                .appendValueReduced(ChronoField.YEAR, 2, 2, 1990)
                .optionalEnd()
                .toFormatter()
    );

    for (DateTimeFormatter pattern : patterns) {
      try {
        return pattern.parse(string, LocalDate::from);
      } catch (DateTimeParseException e) {
        // Try another pattern
      }
    }
    throw new RuntimeException("Failed to parse date: " + string);
  }

  static LocalTime parseTime(String string) {
    List<DateTimeFormatter> patterns = new ArrayList<>();
    patterns.add(DateTimeFormatter.ofPattern("hh:mm a"));
    patterns.add(DateTimeFormatter.ofPattern("hh:mm:ss a"));
    patterns.add(DateTimeFormatter.ofPattern("HH:mm"));
    patterns.add(DateTimeFormatter.ofPattern("HH:mm:ss"));
    // Java 8 Requires AM/PM to be in upper case (Java 11 requires it to be lowercase)
    String upperString = string.toLowerCase();
    for (DateTimeFormatter pattern : patterns) {
      try {
        return pattern.parse(upperString, LocalTime::from);
      } catch (DateTimeParseException e) {
        // Try another pattern.
      }
    }
    throw new RuntimeException("Failed to parse time: " + string);
  }
}
