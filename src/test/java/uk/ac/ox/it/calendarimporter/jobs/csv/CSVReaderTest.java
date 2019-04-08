package uk.ac.ox.it.calendarimporter.jobs.csv;

import static org.junit.Assert.*;

import edu.ksu.canvas.model.CalendarEvent;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class CSVReaderTest {

  private CSVReader csvReader;
  private boolean hasErrors;
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
  }

  private URL getResource(String s) {
    return getClass().getResource(s);
  }

  @Test(expected = RuntimeException.class)
  public void testEmptyImport() throws IOException {
    csvReader.parseCSV(getResource("/empty.csv"), errorHandler);
  }

  @Test
  public void testEmptyBlankFirstLine() throws IOException {
    csvReader.parseCSV(getResource("/blank-first-line.csv"), errorHandler);
  }

  @Test
  public void testSingleEvent() throws IOException {
    List<CalendarEvent> calendarEvents =
        csvReader.parseCSV(getResource("/one-event.csv"), errorHandler);
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
    List<CalendarEvent> calendarEvents =
        csvReader.parseCSV(getResource("/one-event-basics.csv"), errorHandler);
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
    List<CalendarEvent> calendarEvents =
        csvReader.parseCSV(getResource("/two-events.csv"), errorHandler);
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
    List<CalendarEvent> calendarEvents =
        csvReader.parseCSV(getResource("/extra-headers.csv"), errorHandler);
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
    List<CalendarEvent> calendarEvents =
        csvReader.parseCSV(getResource("/extra-values.csv"), errorHandler);
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
    List<CalendarEvent> calendarEvents =
        csvReader.parseCSV(getResource("/whitespace-in-header.csv"), errorHandler);
    assertFalse(hasErrors);
    assertNotNull(calendarEvents);
    assertEquals(1, calendarEvents.size());
    CalendarEvent event = calendarEvents.get(0);
    assertEquals("BulkImport", event.getTitle());
  }

  @Test
  public void testZeroEvents() throws IOException {
    List<CalendarEvent> calendarEvents =
        csvReader.parseCSV(getResource("/zero-events.csv"), errorHandler);
    assertFalse(hasErrors);
    assertNotNull(calendarEvents);
    assertTrue(calendarEvents.isEmpty());
  }

  @Test
  public void testEndTime() throws IOException {
    List<CalendarEvent> calendarEvents =
        csvReader.parseCSV(getResource("/end-time.csv"), errorHandler);
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
    List<CalendarEvent> calendarEvents =
        csvReader.parseCSV(getResource("/end-before-start.csv"), errorHandler);
    assertTrue(hasErrors);
    assertTrue(calendarEvents.isEmpty());
  }

  @Test
  public void testMissingData() throws IOException {
    // Has all the required headers, but is missing essential data on each row.
    List<CalendarEvent> calendarEvents =
        csvReader.parseCSV(getResource("/missing-data.csv"), errorHandler);
    assertTrue(hasErrors);
    assertTrue(calendarEvents.isEmpty());
  }
}
