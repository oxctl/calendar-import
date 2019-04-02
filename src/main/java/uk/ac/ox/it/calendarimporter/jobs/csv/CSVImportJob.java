package uk.ac.ox.it.calendarimporter.jobs.csv;

import static uk.ac.ox.it.calendarimporter.jobs.csv.Field.DATE;
import static uk.ac.ox.it.calendarimporter.jobs.csv.Field.DURATION;
import static uk.ac.ox.it.calendarimporter.jobs.csv.Field.TIME;
import static uk.ac.ox.it.calendarimporter.jobs.csv.Field.TITLE;

import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.jobs.ical.TerminatingInputStream;
import uk.ac.ox.it.calendarimporter.service.EventService;

/**
 * This job reads a CSV from a URL (probably local), parses it and then uploads the events into
 * Canvas.
 */
public class CSVImportJob extends CanvasCalendarJob {

  private Logger log = LoggerFactory.getLogger(CSVImportJob.class);

  private long inputLimit = 1048576 * 10;

  private boolean hasErrors;

  @Autowired private EventService eventService;

  @Override
  public void run() throws IOException, JobExecutionException {
    int progress = 0;
    log(progress, "Import started.");
    URL url = new URL(this.url);
    List<CalendarEvent> calendarEvents = parseCSV(url);
    log("Source file read.");
    CalendarWriter calendarWriter = canvasApiFactory.getWriter(CalendarWriter.class, oauthToken);
    int eventProgress = 0;
    int eventTotal = calendarEvents.size();
    int progressPerEvent = (100 - progress) / eventTotal;
    int created = 0;
    for (CalendarEvent event : calendarEvents) {
      if (isInterrupted()) {
        log("Interrupted after %d of %d events", ++eventProgress, eventTotal);
        throw new JobExecutionException("Job interrupted");
      }
      event.setContextCode(context);
      Optional<CalendarEvent> calendarEventOpt = calendarWriter.createCalendarEvent(event);
      if (calendarEventOpt.isPresent()) {
        eventService.eventCreated(tenant.getId(), calendarImport, calendarEventOpt.get());
        created++;
      }

      progress += progressPerEvent;
      log(progress, "Processed event %d of %d", ++eventProgress, eventTotal);
    }
    log("Completed import, found %d events, imported %d into calendar.", eventTotal, created);
  }

  public List<CalendarEvent> parseCSV(URL url) throws IOException {
    try {
      log.debug("Attempting to load CSV file: {}", url);
      URLConnection connection = url.openConnection();
      connection.setReadTimeout(10000);
      try (InputStream in = new TerminatingInputStream(connection.getInputStream(), inputLimit)) {
        // Ignore blank lines
        CSVFormat format = CSVFormat.EXCEL
                .withFirstRecordAsHeader()
                .withIgnoreEmptyLines()
                .withIgnoreSurroundingSpaces();
        CSVParser parser = new CSVParser(new InputStreamReader(in, StandardCharsets.UTF_8), format);
        validateHeader(parser.getHeaderMap().keySet());
        List<CalendarEvent> events = new ArrayList<>();
        for (CSVRecord record : parser) {
          try {
            validateRow(record);
            CalendarEvent calendarEvent = parseRecord(record);
            events.add(calendarEvent);
          } catch (RowException e) {
            // Programmers are 0 based, users are 1 based
            log("Error on row %d, message: %s", e.getRow()+1, e.getLocalizedMessage());
            hasErrors = true;
          }
        }
        log.trace("Parsed {} rows and has errors: {}", events.size(), hasErrors);
        return events;
      }
    } catch (IOException e) {
      throw e;
    }
  }

  private CalendarEvent parseRecord(CSVRecord record) {
    try {
      CalendarEvent event = new CalendarEvent();
      event.setTitle(record.get(TITLE));
      Duration duration = DateTimeParser.parseDuration(record.get(DURATION).trim());
      LocalTime time = DateTimeParser.parseTime(record.get(TIME).trim());
      LocalDate date = DateTimeParser.parseDate(record.get(DATE).trim());
      LocalDateTime dateTime = LocalDateTime.of(date, time);
      // TODO Timezone
      Instant starts = dateTime.atZone(ZoneId.systemDefault()).toInstant();
      Instant ends = starts.plus(duration);
      event.setStartAt(starts);
      event.setEndAt(ends);
      event.setAllDay(false);
      return event;
    } catch (RuntimeException e) {
      throw new RowException(record.getRecordNumber(), e.getLocalizedMessage());
    }
  }

  private void validateRow(CSVRecord record) {
    for (Field field : Field.values()) {
      if (field.isRequired()) {
        String s = null;
        try {
          s = record.get(field);
        } catch (IllegalArgumentException e) {
          // Ignore
        }
        if (s == null || s.trim().isEmpty()) {
          throw new RowException(
              record.getRecordNumber(), "Missing required field: " + field.getHeader());
        }
      }
    }
  }

  private void validateHeader(Set<String> headers) {
    for (Field field : Field.values()) {
      if (field.isRequired() && !headers.contains(field.getHeader())) {
        throw new RuntimeException("Missing required header: " + field.getHeader());
      }
    }
  }

  public boolean hasErrors() {
    return hasErrors;
  }
}
