package uk.ac.ox.it.calendarimporter.jobs.csv;

import static org.junit.Assert.*;

import edu.ksu.canvas.model.CalendarEvent;
import java.io.IOException;
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
          e.printStackTrace();
          hasErrors = true;
        }
      };

  @Before
  public void setUp() {
    csvReader = new CSVReader();
    hasErrors = false;
  }

  @Test(expected = RuntimeException.class)
  public void testEmptyImport() throws IOException {
    csvReader.parseCSV(getClass().getResource("/empty.csv"), errorHandler);
  }

  @Test
  public void testEmptyBlankFirstLine() throws IOException {
    csvReader.parseCSV(getClass().getResource("/blank-first-line.csv"), errorHandler);
  }

  @Test
  public void testSingleEvent() throws IOException {
    List<CalendarEvent> calendarEvents =
        csvReader.parseCSV(getClass().getResource("/one-event.csv"), errorHandler);
    assertFalse(hasErrors);
    assertNotNull(calendarEvents);
    assertEquals(1, calendarEvents.size());
    CalendarEvent event = calendarEvents.get(0);
    assertEquals("Event Title", event.getTitle());
    assertEquals(0, event.getStartAt().getEpochSecond());
    assertEquals(0, event.getEndAt().getEpochSecond());
  }

  @Test
  public void testMultipleEvents() throws IOException {
    List<CalendarEvent> calendarEvents =
        csvReader.parseCSV(getClass().getResource("/two-events.csv"), errorHandler);
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
        csvReader.parseCSV(getClass().getResource("/extra-headers.csv"), errorHandler);
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
        csvReader.parseCSV(getClass().getResource("/extra-values.csv"), errorHandler);
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
        csvReader.parseCSV(getClass().getResource("/whitespace-in-header.csv"), errorHandler);
    assertFalse(hasErrors);
    assertNotNull(calendarEvents);
    assertEquals(1, calendarEvents.size());
    CalendarEvent event = calendarEvents.get(0);
    assertEquals("BulkImport", event.getTitle());
  }
}
