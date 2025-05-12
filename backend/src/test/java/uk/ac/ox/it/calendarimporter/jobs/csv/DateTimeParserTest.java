package uk.ac.ox.it.calendarimporter.jobs.csv;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DateTimeParserTest {

    @Test
    public void testDurationParsingEmpty() {
        assertThrows(RuntimeException.class, () -> DateTimeParser.parseDuration(""));
    }

    @Test
    public void testNegativeDuration() {
        assertThrows(RuntimeException.class, () -> DateTimeParser.parseDuration("-10"));
    }

    @Test
    public void testNegativeHoursDuration() {
        assertThrows(RuntimeException.class, () -> DateTimeParser.parseDuration("-1:120"));
    }

    @Test
    public void testDurtionParsingMinutes() {
        Duration duration = DateTimeParser.parseDuration("30");
        assertEquals(Duration.ofMinutes(30), duration);
    }

    @Test
    public void testDurationParsingHours() {
        Duration duration = DateTimeParser.parseDuration("10:0");
        assertEquals(Duration.ofHours(10), duration);
    }

    @Test
    public void testDurationParsingHoursMinutes() {
        Duration duration = DateTimeParser.parseDuration("10:10");
        assertEquals(Duration.ofMinutes(610), duration);
    }

    @Test
    public void testAdjustHours() {
        Duration duration = DateTimeParser.parseDuration("2:-5");
        assertEquals(Duration.ofMinutes(115), duration);
    }

    // The Date Tests

    @Test
    public void testParseDate() {
        LocalDate date = DateTimeParser.parseDate("01/01/1970");
        assertEquals(LocalDate.of(1970, 1, 1), date);
    }

    @Test
    public void testParseDateLater() {
        LocalDate date = DateTimeParser.parseDate("15/01/2018");
        assertEquals(LocalDate.of(2018, 1, 15), date);
    }

    @Test
    public void testParseSingleYear() {
        LocalDate date = DateTimeParser.parseDate("15/01/18");
        assertEquals(LocalDate.of(2018, 1, 15), date);
    }

    @Test
    public void testParseNoYear() {
        assertThrows(
                RuntimeException.class,
                () -> {
                    LocalDate date = DateTimeParser.parseDate("15/01/");
                    assertEquals(LocalDate.of(2018, 1, 15), date);
                });
    }

    @Test
    public void testParseNoYearOrSlash() {
        assertThrows(
                RuntimeException.class,
                () -> {
                    LocalDate date = DateTimeParser.parseDate("15/01");
                    assertEquals(LocalDate.of(2018, 1, 15), date);
                });
    }

    @Test
    public void testParseDateShort() {
        LocalDate date = DateTimeParser.parseDate("1/1/1970");
        assertEquals(LocalDate.of(1970, 1, 1), date);
    }

    // The Time Tests

    @Test
    public void testParseTime() {
        LocalTime time = DateTimeParser.parseTime("12:00 PM");
        assertEquals(LocalTime.of(12, 0), time);
    }

    @Test
    public void testParseTimeLowercase() {
        LocalTime time = DateTimeParser.parseTime("12:00 pm");
        assertEquals(LocalTime.of(12, 0), time);
    }

    @Test
    public void testParseTime24() {
        LocalTime time = DateTimeParser.parseTime("12:00");
        assertEquals(LocalTime.of(12, 0), time);
    }

    @Test
    public void testParseTimeAfternoon() {
        LocalTime time = DateTimeParser.parseTime("05:00 PM");
        assertEquals(LocalTime.of(17, 0), time);
    }

    @Test
    public void testParseTime24Afternoon() {
        LocalTime time = DateTimeParser.parseTime("17:00");
        assertEquals(LocalTime.of(17, 0), time);
    }

    @Test
    public void testParseTimeSecond() {
        LocalTime time = DateTimeParser.parseTime("10:20:30 AM");
        assertEquals(LocalTime.of(10, 20, 30), time);
    }

    @Test
    public void testParseTime24Second() {
        LocalTime time = DateTimeParser.parseTime("10:20:30");
        assertEquals(LocalTime.of(10, 20, 30), time);
    }

    @Test
    public void testParseTimeSingleHour() {
        assertThrows(
                RuntimeException.class,
                () -> {
                    LocalTime time = DateTimeParser.parseTime("1:00");
                });
    }

    @Test
    public void testParseTimeSingleMinute() {
        assertThrows(
                RuntimeException.class,
                () -> {
                    LocalTime time = DateTimeParser.parseTime("01:0");
                });
    }

    @Test
    public void testParseTimeSingleSecond() {
        assertThrows(
                RuntimeException.class,
                () -> {
                    LocalTime time = DateTimeParser.parseTime("01:00:0");
                });
    }

    @Test
    public void testParseISODate() {
        LocalDate date = DateTimeParser.parseDate("2023-01-15");
        assertEquals(LocalDate.of(2023, 1, 15), date);
    }

    @Test
    public void testParseTwoDigitMonthDayUKFormat() {
        LocalDate date = DateTimeParser.parseDate("15/01/2023");
        assertEquals(LocalDate.of(2023, 1, 15), date);
    }

    @Test
    public void testParseSingleDigitMonthDayUKFormat() {
        LocalDate date = DateTimeParser.parseDate("5/1/2023");
        assertEquals(LocalDate.of(2023, 1, 5), date);
    }

    @Test
    public void testParseTwoDigitYearUKFormat() {
        LocalDate date = DateTimeParser.parseDate("15/01/23");
        assertEquals(LocalDate.of(2023, 1, 15), date);
    }

    @Test
    public void testParseSingleDigitMonthDayTwoDigitYearUKFormat() {
        LocalDate date = DateTimeParser.parseDate("5/1/23");
        assertEquals(LocalDate.of(2023, 1, 5), date);
    }

    // testing that "invalid" dates throw an exception
    @Test
    public void testParseUSDateThrowsException() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> DateTimeParser.parseDate("01/15/2023")
        );
        assertEquals(
                "Failed to parse date: 01/15/2023. Please use format DD/MM/YYYY or YYYY-MM-DD",
                exception.getMessage()
        );
    }
}
