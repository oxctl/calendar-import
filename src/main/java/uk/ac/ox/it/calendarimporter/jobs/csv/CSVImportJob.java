package uk.ac.ox.it.calendarimporter.jobs.csv;

import edu.ksu.canvas.interfaces.CalendarWriter;
import edu.ksu.canvas.model.CalendarEvent;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.quartz.JobExecutionException;
import uk.ac.ox.it.calendarimporter.jobs.CanvasCalendarJob;
import uk.ac.ox.it.calendarimporter.jobs.ical.TerminatingInputStream;

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
import java.util.Set;

import static uk.ac.ox.it.calendarimporter.jobs.csv.Field.*;

public class CSVImportJob extends CanvasCalendarJob {

    private long inputLimit = 1048576 * 10;

    private List<String> errors = new ArrayList<>();

    @Override
    public void run() throws IOException, JobExecutionException {
        URL url = new URL(this.url);
        List<CalendarEvent> calendarEvents = parseCSV(url);
        CalendarWriter calendarWriter = canvasApiFactory.getWriter(CalendarWriter.class, nonRefreshableOauthToken);
        for (CalendarEvent event: calendarEvents) {
            if (isInterrupted()) {
                throw new JobExecutionException("Job interrupted");
            }
            event.setContextCode(context);
            calendarWriter.createCalendarEvent(event);
        }
    }

    public List<CalendarEvent> parseCSV(URL url) throws IOException {
        try {
            URLConnection connection = url.openConnection();
            connection.setReadTimeout(10000);
            try (InputStream in =  new TerminatingInputStream(connection.getInputStream(), inputLimit)) {
                // Ignore blank lines
                CSVFormat format = CSVFormat.EXCEL.withFirstRecordAsHeader().withIgnoreEmptyLines();
                CSVParser parser = new CSVParser(new InputStreamReader(in, StandardCharsets.UTF_8),format);
                validateHeader(parser.getHeaderMap().keySet());
                List<CalendarEvent> events = new ArrayList<>();
                for (CSVRecord record : parser) {
                    try {
                        validateRow(record);
                        CalendarEvent calendarEvent = parseRecord(record);
                        events.add(calendarEvent);
                    } catch (RowException e) {
                        saveError(e);
                    }
                }
                return events;
            }
        } catch (IOException e) {
            throw e;
        }
    }

    private void saveError(RowException e) {
        errors.add("Error on "+ e.getRow()+ " "+ e.getLocalizedMessage());
    }

    private CalendarEvent parseRecord(CSVRecord record) {
        try {
            CalendarEvent event = new CalendarEvent();
            validateRow(record);
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
                String s = record.get(field);
                if (s == null || s.trim().isEmpty()) {
                    throw new RuntimeException("Missing required field: "+ field.getHeader());
                }
            }
        }
    }

    private void validateHeader(Set<String> headers) {
        // TODO Duplicate header value check?
        for (Field field : Field.values()) {
            if (field.isRequired() && ! headers.contains(field.getHeader())) {
                throw new RuntimeException("Missing required header: "+ field.getHeader());
            }
        }
    }
}
