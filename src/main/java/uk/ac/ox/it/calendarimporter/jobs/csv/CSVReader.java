package uk.ac.ox.it.calendarimporter.jobs.csv;

import static uk.ac.ox.it.calendarimporter.jobs.csv.Field.*;

import edu.ksu.canvas.model.CalendarEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import uk.ac.ox.it.calendarimporter.jobs.ical.TerminatingInputStream;

/**
 * This reads in the CSV file calling the handler for any errors.
 *
 * @see RowException The exception for any problems we find.
 */
@Component
public class CSVReader {

  /** Callback to handle an error. */
  public interface ErrorHandler {
    void handleError(RowException e);
  }

  @Value("${spring.servlet.multipart.max-file-size:10MB}")
  private DataSize inputLimit = DataSize.ofMegabytes(10);

  public List<CalendarEvent> parseCSV(URL url, TimeZone timeZone, ErrorHandler errorHandler) throws IOException {
    try {
      URLConnection connection = url.openConnection();
      connection.setReadTimeout(10000);
      try (InputStream in =
          new TerminatingInputStream(connection.getInputStream(), inputLimit.toBytes())) {
        // Ignore blank lines
        CSVFormat format =
            CSVFormat.EXCEL
                .withFirstRecordAsHeader()
                .withIgnoreEmptyLines()
                .withIgnoreSurroundingSpaces();
        CSVParser parser = new CSVParser(new InputStreamReader(in, StandardCharsets.UTF_8), format);
        validateHeader(parser.getHeaderMap().keySet());
        List<CalendarEvent> events = new ArrayList<>();
        for (CSVRecord record : parser) {
          try {
            validateRow(record);
            CalendarEvent calendarEvent = parseRecord(record, timeZone);
            events.add(calendarEvent);
          } catch (RowException e) {
            errorHandler.handleError(e);
          }
        }
        return events;
      }
    } catch (IOException e) {
      throw e;
    }
  }

  private CalendarEvent parseRecord(CSVRecord record, TimeZone timeZone) {
    try {
      CalendarEvent event = new CalendarEvent();
      event.setTitle(get(record, TITLE));
      LocalTime time = DateTimeParser.parseTime(get(record, TIME).trim());
      LocalDate date = DateTimeParser.parseDate(get(record, DATE).trim());
      LocalDateTime dateTime = LocalDateTime.of(date, time);
      Instant starts = dateTime.atZone(timeZone.toZoneId()).toInstant();
      Instant ends = null;
      String durationStr = get(record, DURATION);
      if (durationStr != null && !durationStr.isBlank()) {
        Duration duration = DateTimeParser.parseDuration(durationStr.trim());
        ends = starts.plus(duration);
      }
      String endTimeStr = get(record, END_TIME);
      if (endTimeStr != null && !endTimeStr.isBlank()) {
        LocalTime endTime = DateTimeParser.parseTime(endTimeStr.trim());
        LocalDateTime endDateTime = LocalDateTime.of(date, endTime);
        ends = endDateTime.atZone(timeZone.toZoneId()).toInstant();
        if (!starts.isBefore(ends)) {
          throw new RowException(record.getRecordNumber(), "Start time cannot be after end time.");
        }
      }
      event.setDescription(get(record, DESCRIPTION));
      event.setLocationName(get(record, LOCATION));
      event.setLocationAddress(get(record, ADDRESS));
      event.setStartAt(starts);
      event.setEndAt(ends);
      event.setAllDay(false);
      return event;
    } catch (RuntimeException e) {
      throw new RowException(record.getRecordNumber(), e.getLocalizedMessage());
    }
  }

  private String get(CSVRecord record, Field field) {
    try {
      return record.get(field);
    } catch (IllegalArgumentException e) {
      if (field.isRequired()) {
        throw e;
      }
    }
    return null;
  }

  private void validateRow(CSVRecord record) {
    // We want either end time or duration, not both
    boolean knowEnd = false;
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
      } else {
        String value = get(record, field);
        if (value != null && !value.isBlank()) {
          switch (field) {
            case DURATION:
            case END_TIME:
              if (knowEnd) {
                throw new RowException(
                    record.getRecordNumber(), "Cannot have both end time and duration set.");
              }
              knowEnd = true;
              break;
          }
        }
      }
    }
    if (!knowEnd) {
      throw new RowException(
          record.getRecordNumber(), "You must have either end time or duration set.");
    }
  }

  private void validateHeader(Set<String> headers) {
    for (Field field : Field.values()) {
      if (field.isRequired() && !headers.contains(field.getHeader())) {
        throw new RuntimeException("Missing required header: " + field.getHeader());
      }
    }
  }
}
