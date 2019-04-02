package uk.ac.ox.it.calendarimporter.jobs.csv;

import static org.junit.Assert.*;

import edu.ksu.canvas.model.CalendarEvent;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class CSVCalendarImportJobTest {

  private CSVImportJob csvImportJob;

  @Before
  public void setUp() {
    csvImportJob = new CSVImportJob();
  }

  @Test(expected = RuntimeException.class)
  public void testEmptyImport() throws IOException {
    csvImportJob.parseCSV(getClass().getResource("/empty.csv"));
  }

  @Test
  public void testEmptyBlankFirstLine() throws IOException {
    csvImportJob.parseCSV(getClass().getResource("/blank-first-line.csv"));
  }

  @Test
  public void testSingleEvent() throws IOException {
    List<CalendarEvent> calendarEvents =
        csvImportJob.parseCSV(getClass().getResource("/one-event.csv"));
    assertFalse(csvImportJob.hasErrors());
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
        csvImportJob.parseCSV(getClass().getResource("/two-events.csv"));
    assertFalse(csvImportJob.hasErrors());
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
        csvImportJob.parseCSV(getClass().getResource("/extra-headers.csv"));
    assertFalse(csvImportJob.hasErrors());
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
        csvImportJob.parseCSV(getClass().getResource("/extra-values.csv"));
    assertFalse(csvImportJob.hasErrors());
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
            csvImportJob.parseCSV(getClass().getResource("/whitespace-in-header.csv"));
    assertFalse(csvImportJob.hasErrors());
    assertNotNull(calendarEvents);
    assertEquals(1, calendarEvents.size());
    CalendarEvent event = calendarEvents.get(0);
    assertEquals("BulkImport", event.getTitle());
  }
}
