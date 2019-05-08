package uk.ac.ox.it.calendarimporter.jobs.csv;

import static org.junit.Assert.assertEquals;

import java.time.*;
import org.junit.Test;

public class DateTimeParserTest {

  @Test(expected = RuntimeException.class)
  public void testDurationParsingEmpty() {
    DateTimeParser.parseDuration("");
  }

  @Test(expected = RuntimeException.class)
  public void testNegativeDuration() {
    DateTimeParser.parseDuration("-10");
  }

  @Test(expected = RuntimeException.class)
  public void testNegativeHoursDuration() {
    DateTimeParser.parseDuration("-1:120");
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

  @Test(expected = RuntimeException.class)
  public void testParseNoYear() {
    LocalDate date = DateTimeParser.parseDate("15/01/");
    assertEquals(LocalDate.of(2018, 1, 15), date);
  }

  @Test(expected = RuntimeException.class)
  public void testParseNoYearOrSlash() {
    LocalDate date = DateTimeParser.parseDate("15/01");
    assertEquals(LocalDate.of(2018, 1, 15), date);
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

  @Test(expected = RuntimeException.class)
  public void testParseTimeSingleHour() {
    LocalTime time = DateTimeParser.parseTime("1:00");
  }

  @Test(expected = RuntimeException.class)
  public void testParseTimeSingleMinute() {
    LocalTime time = DateTimeParser.parseTime("01:0");
  }

  @Test(expected = RuntimeException.class)
  public void testParseTimeSingleSecond() {
    LocalTime time = DateTimeParser.parseTime("01:00:0");
  }
}
