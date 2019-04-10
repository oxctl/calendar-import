package uk.ac.ox.it.calendarimporter.jobs.csv;

import static org.junit.Assert.*;

import edu.ksu.canvas.model.CalendarEvent;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;

public class CSVReaderTest {

  private CSVReader csvReader;
  private boolean hasErrors;
  private TimeZone timeZone;
  private CSVReader.ErrorHandler errorHandler =
      new CSVReader.ErrorHandler() {
        @Override
        public void handleError(RowException e) {
          hasErrors = true;
        }
      };

  @Before
  public void setUp() {
    csvReader = new CSVReader();
    hasErrors = false;
    timeZone = TimeZone.getTimeZone("Europe/London");
  }

  private List<CalendarEvent> parse(String s) throws IOException {
    return csvReader.parseCSV(getResource(s), timeZone, errorHandler);
  }

  private URL getResource(String s) {
    return getClass().getResource(s);
  }

  @Test(expected = RuntimeException.class)
  public void testEmptyImport() throws IOException {
    parse("/empty.csv");
  }

  @Test
  public void testEmptyBlankFirstLine() throws IOException {
    parse("/blank-first-line.csv");
  }

  @Test
  public void testSingleEvent() throws IOException {
    List<CalendarEvent> calendarEvents = parse("/one-event.csv");
    assertFalse(hasErrors);
    assertNotNull(calendarEvents);
    assertEquals(1, calendarEvents.size());
    CalendarEvent event = calendarEvents.get(0);
    assertEquals("Event Title", event.getTitle());
    assertEquals(0, event.getStartAt().getEpochSecond());
    assertEquals(0, event.getEndAt().getEpochSecond());
    assertEquals("Event Description", event.getDescription());
    assertEquals("Event Location", event.getLocationName());
    assertEquals("Event Address", event.getLocationAddress());
  }


  @Test
  public void testSingleEventBasics() throws IOException {
    List<CalendarEvent> calendarEvents = parse("/one-event-basics.csv");
    assertFalse(hasErrors);
    assertNotNull(calendarEvents);
    assertEquals(1, calendarEvents.size());
    CalendarEvent event = calendarEvents.get(0);
    assertEquals("Event Title", event.getTitle());
    assertEquals(0, event.getStartAt().getEpochSecond());
    assertEquals(0, event.getEndAt().getEpochSecond());
    assertNull(event.getDescription());
    assertNull(event.getLocationName());
    assertNull(event.getLocationAddress());
  }

  @Test
  public void testMultipleEvents() throws IOException {
    List<CalendarEvent> calendarEvents = parse("/two-events.csv");
    assertFalse(hasErrors);
    assertNotNull(calendarEvents);
    assertEquals(2, calendarEvents.size());
    {
      CalendarEvent event = calendarEvents.get(0);
      assertEquals("Event Title 1", event.getTitle());
    }
    {
      CalendarEvent event = calendarEvents.get(1);
      assertEquals("Event Title 2", event.getTitle());
    }
  }

  @Test
  public void testIgnoredHeaders() throws IOException {
    List<CalendarEvent> calendarEvents = parse("/extra-headers.csv");
    assertFalse(hasErrors);
    assertNotNull(calendarEvents);
    assertEquals(1, calendarEvents.size());
    CalendarEvent event = calendarEvents.get(0);
    assertEquals("Event Title", event.getTitle());
    assertEquals(0, event.getStartAt().getEpochSecond());
    assertEquals(0, event.getEndAt().getEpochSecond());
  }

  @Test
  public void testIgnoredRowValues() throws IOException {
    List<CalendarEvent> calendarEvents = parse("/extra-values.csv");
    assertFalse(hasErrors);
    assertNotNull(calendarEvents);
    assertEquals(1, calendarEvents.size());
    CalendarEvent event = calendarEvents.get(0);
    assertEquals("Event Title", event.getTitle());
    assertEquals(0, event.getStartAt().getEpochSecond());
    assertEquals(0, event.getEndAt().getEpochSecond());
  }

  @Test
  public void testWhitespaceInHeader() throws IOException {
    List<CalendarEvent> calendarEvents = parse("/whitespace-in-header.csv");
    assertFalse(hasErrors);
    assertNotNull(calendarEvents);
    assertEquals(1, calendarEvents.size());
    CalendarEvent event = calendarEvents.get(0);
    assertEquals("BulkImport", event.getTitle());
  }

  @Test
  public void testZeroEvents() throws IOException {
    List<CalendarEvent> calendarEvents = parse("/zero-events.csv");
    assertFalse(hasErrors);
    assertNotNull(calendarEvents);
    assertTrue(calendarEvents.isEmpty());
  }

  @Test
  public void testEndTime() throws IOException {
    List<CalendarEvent> calendarEvents = parse("/end-time.csv");
    assertFalse(hasErrors);
    assertNotNull(calendarEvents);
    assertEquals(1, calendarEvents.size());
    CalendarEvent event = calendarEvents.get(0);
    assertEquals("Event Title", event.getTitle());
    assertEquals(0, event.getStartAt().getEpochSecond());
    assertEquals(3600, event.getEndAt().getEpochSecond());
  }

  @Test
  public void testEndBeforeStart() throws IOException {
    List<CalendarEvent> calendarEvents = parse("/end-before-start.csv");
    assertTrue(hasErrors);
    assertTrue(calendarEvents.isEmpty());
  }

  @Test
  public void testMissingData() throws IOException {
    // Has all the required headers, but is missing essential data on each row.
    List<CalendarEvent> calendarEvents = parse("/missing-data.csv");
    assertTrue(hasErrors);
    assertTrue(calendarEvents.isEmpty());
  }

  @Test
  public void testDurationTooLong() throws IOException {
    // Has all the required headers, but is missing essential data on each row.
    List<CalendarEvent> calendarEvents = parse("/too-long-duration.csv");
    assertTrue(hasErrors);
    assertTrue(calendarEvents.isEmpty());
  }
}
