package uk.ac.ox.it.calendarimporter.jobs.csv;

import edu.ksu.canvas.model.CalendarEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

public class CSVReaderTest {

    private CSVReader csvReader;
    private boolean hasErrors;
    private TimeZone timeZone;
    private final CSVReader.ErrorHandler errorHandler =
            new CSVReader.ErrorHandler() {
                @Override
                public void handleError(RowException e) {
                    hasErrors = true;
                }
            };

    @BeforeEach
    public void setUp() {
        csvReader = new CSVReader();
        hasErrors = false;
        timeZone = TimeZone.getTimeZone("Europe/London");
    }

    private List<CalendarEvent> parse(String s) throws IOException, HeaderException {
        return csvReader.parseCSV(getResource(s).openStream(), timeZone, errorHandler);
    }

    private URL getResource(String s) {
        return getClass().getResource(s);
    }

    @Test
    public void testEmptyImport() {
        assertThrows(HeaderException.class, () -> parse("/empty.csv"));
    }

    @Test
    public void testEmptyBlankFirstLine() throws IOException, HeaderException {
        parse("/blank-first-line.csv");
    }

    @Test
    public void testSingleEvent() throws IOException, HeaderException {
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
    public void testSingleEventBasics() throws IOException, HeaderException {
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
    public void testAdditionalEmptyLines() throws IOException, HeaderException {
        List<CalendarEvent> calendarEvents = parse("/additional-empty-lines.csv");
        assertFalse(hasErrors);
        assertNotNull(calendarEvents);
        assertEquals(1, calendarEvents.size());
    }

    @Test
    public void testMultipleEvents() throws IOException, HeaderException {
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
    public void testIgnoredHeaders() throws IOException, HeaderException {
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
    public void testIgnoredRowValues() throws IOException, HeaderException {
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
    public void testWhitespaceInHeader() throws IOException, HeaderException {
        List<CalendarEvent> calendarEvents = parse("/whitespace-in-header.csv");
        assertFalse(hasErrors);
        assertNotNull(calendarEvents);
        assertEquals(1, calendarEvents.size());
        CalendarEvent event = calendarEvents.get(0);
        assertEquals("BulkImport", event.getTitle());
    }

    @Test
    public void testZeroEvents() throws IOException, HeaderException {
        List<CalendarEvent> calendarEvents = parse("/zero-events.csv");
        assertFalse(hasErrors);
        assertNotNull(calendarEvents);
        assertTrue(calendarEvents.isEmpty());
    }

    @Test
    public void testEndTime() throws IOException, HeaderException {
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
    public void testEndBeforeStart() throws IOException, HeaderException {
        List<CalendarEvent> calendarEvents = parse("/end-before-start.csv");
        assertTrue(hasErrors);
        assertTrue(calendarEvents.isEmpty());
    }

    @Test
    public void testMissingData() throws IOException, HeaderException {
        // Has all the required headers, but is missing essential data on each row.
        List<CalendarEvent> calendarEvents = parse("/missing-data.csv");
        assertTrue(hasErrors);
        assertTrue(calendarEvents.isEmpty());
    }

    @Test
    public void testMoreThan24Hours() throws IOException, HeaderException {
        List<CalendarEvent> calendarEvents = parse("/more-than24-hours.csv");
        assertFalse(hasErrors);
        assertFalse(calendarEvents.isEmpty());
    }

    @Test
    public void testSameStartAndEnd() throws IOException, HeaderException {
        // Has all the required headers, but is missing essential data on each row.
        List<CalendarEvent> calendarEvents = parse("/same-start-end.csv");
        assertFalse(hasErrors);
        assertEquals(1, calendarEvents.size());
        CalendarEvent event = calendarEvents.get(0);
        assertEquals("Event Title", event.getTitle());
        assertEquals(0, event.getStartAt().getEpochSecond());
        assertEquals(0, event.getEndAt().getEpochSecond());
    }

    @Test
    public void testZeroDuration() throws IOException, HeaderException {
        // Has all the required headers, but is missing essential data on each row.
        List<CalendarEvent> calendarEvents = parse("/zero-length-event.csv");
        assertFalse(hasErrors);
        assertEquals(1, calendarEvents.size());
        CalendarEvent event = calendarEvents.get(0);
        assertEquals("Event Title", event.getTitle());
        assertEquals(0, event.getStartAt().getEpochSecond());
        assertEquals(0, event.getEndAt().getEpochSecond());
    }
}
